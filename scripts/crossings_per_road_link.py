# Script to analyse which road links pedestrian agents cross on

import json
import os
import pandas as pd
import numpy as np
import geopandas as gpd
import re
import networkx as nx
from datetime import datetime as dt

import batch_data_utils as bd_utils

#####################################
#
# Globals
#
#####################################
project_crs = {'init': 'epsg:27700'}

with open(".//gis_data_processing//config.json") as f:
    config = json.load(f)

gis_data_dir = os.path.abspath("..\\data\\model_gis_data")
data_dir = config['batch_data_dir']
img_dir = ".\\output\\img\\"
l_re = re.compile(r"(\d+\.\d+),\s(\d+\.\d+)")

pavement_links_file = os.path.join(gis_data_dir, config['pavement_links_file'])
or_links_file = os.path.join(gis_data_dir, config['openroads_link_processed_file'])
or_nodes_file = os.path.join(gis_data_dir, config['openroads_node_processed_file'])


# Model output data
file_datetime_string = "2021.May.28.17_57_01"
file_datetime  =dt.strptime(file_datetime_string, "%Y.%b.%d.%H_%M_%S")
file_re = bd_utils.get_file_regex("pedestrian_pave_link_crossings", file_datetime = None)
ped_crossings_file = os.path.join(data_dir, bd_utils.most_recent_directory_file(data_dir, file_re))

file_re = bd_utils.get_file_regex("pedestrian_pave_link_crossings", file_datetime = None, suffix = 'batch_param_map')
batch_file = bd_utils.most_recent_directory_file(data_dir, file_re)

# Output paths
img_dir = "..\\output\\img\\"
crossing_network_fig = os.path.join(img_dir, "crossing_network.png")


#####################################
#
#
# Load Data
#
#
#####################################

# Data from model run
dfPedCrossings = pd.read_csv(ped_crossings_file)
dfRun = pd.read_csv(os.path.join(data_dir, batch_file))

# GIS Data
gdfPaveNetwork = gpd.read_file(pavement_links_file)
gdfORLinks = gpd.read_file(or_links_file)
gdfORNodes = gpd.read_file(or_nodes_file)

# Get networkx graph and node positions
G = nx.Graph()
edges = gdfORLinks.loc[:, ['MNodeFID', 'PNodeFID', 'fid']].values
G.add_weighted_edges_from(edges, weight='fid')

# Using the geographical coordinates of the nodes when plotting them
points_pos = gdfORNodes.set_index('node_fid')
points_pos['x'] = points_pos['geometry'].map(lambda g: g.coords[0][0])
points_pos['y'] = points_pos['geometry'].map(lambda g: g.coords[0][1])
node_posistions = list(zip(points_pos['x'], points_pos['y']))
dict_node_pos = dict(zip(points_pos.index, node_posistions))

#######################################
#
#
# Process data
#
#
########################################

# Filter to just include pavement links used for crossing
dfPedCrossings = dfPedCrossings.loc[ ~dfPedCrossings['TraversedPavementLinkID'].isnull()]
dfPedCrossings = dfPedCrossings.merge(gdfPaveNetwork, left_on = 'TraversedPavementLinkID', right_on = 'fid', how = 'left', indicator=True)
assert dfPedCrossings.loc[ dfPedCrossings['_merge']!='both'].shape[0]==0
dfPedCrossings.drop('_merge', axis=1, inplace=True)

# Aggregate crossing counts
dfCrossingCounts = dfPedCrossings.groupby(['run', 'pedRLID']).apply(lambda g: g.shape[0]).reset_index()
dfCrossingCounts.rename(columns = {0:'cross_count'}, inplace=True)

# Join with pavement Road link data and run data
gdfCrossingCounts = pd.merge(gdfORLinks, dfCrossingCounts, left_on = 'fid', right_on = 'pedRLID', how = 'left')
gdfCrossingCounts['cross_count'] = gdfCrossingCounts['cross_count'].fillna(0)
gdfCrossingCounts = pd.merge(gdfCrossingCounts, dfRun, on = 'run')

# Map crossing counts to range for colormap
max_cc = gdfCrossingCounts['cross_count'].max()
gdfCrossingCounts['cmap_value'] = gdfCrossingCounts['cross_count'].map(lambda c: 255*(c/max_cc))

##########################################
#
#
# Plot network
#
#
##########################################

# Plot
from matplotlib import cm # for generating colour maps
from matplotlib import pyplot as plt

def road_network_figure(G, dict_node_pos, dict_edge_values, title, cmap_name = 'viridis', edge_width = 3, edge_alpha = 1):

    plt.style.use('dark_background')
    f, ax = plt.subplots(1,1,figsize = (15,15))

    ax = road_network_subfigure(ax, G, dict_node_pos, dict_edge_values, title, cmap_name = cmap_name, edge_width=edge_width, edge_alpha=edge_alpha)
    return f

def road_network_subfigure(ax, G, dict_node_pos, dict_edge_values, title, cmap_name = 'viridis', edge_width = 3, edge_alpha = 1):
    # Get edge colour map based on number of crossings
    cmap = cm.get_cmap(cmap_name)
    edge_palette = []
    for e in G.edges(data=True):
        fid = e[-1]['fid']
        if fid in dict_edge_values.keys():
            value = dict_edge_values[fid]
        else:
            value = 0

        edge_palette.append(cmap(value))

    nx.draw_networkx_nodes(G, dict_node_pos, ax = ax, nodelist=G.nodes(), node_color = 'grey', node_size = 1, alpha = 0.5)
    nx.draw_networkx_edges(G, dict_node_pos, ax = ax, edgelist=G.edges(), width = 3, edge_color = edge_palette, alpha=1)
    ax.set_title(title)
    ax.axis('off')
    return ax

def batch_runs_road_network_figure(G, dict_node_pos, dfBatch, groupby_columns, fig_title, cmap_name = 'viridis', edge_width = 3, edge_alpha = 1):
    '''Loop through batch run groups and get edge pallet data for each group. Use this to make road crossings
    figure for each group.
    '''

    grouped = dfBatch.groupby(groupby_columns)
    keys = list(grouped.groups.keys())

    # Want to get separate array of data for each value of
    p = len(dfBatch[groupby_columns[0]].unique())
    q = len(dfBatch[groupby_columns[1]].unique())

    key_indices = np.reshape(np.arange(len(keys)), (p,q))

    plt.style.use('dark_background')
    f, axs = plt.subplots(p, q, figsize = (20,20), sharey=False, sharex=False)

    # Make sure axes array in shame that matches the layout
    axs = np.reshape(axs, (p, q))

    # Select data to work with and corresponding axis
    for pi in range(p):
        for qi in range(q):
            key_index = key_indices[pi, qi]
            group_key = keys[key_index]
            dfRun = grouped.get_group(group_key)
            dict_edge_values = dfRun.set_index('fid')['cmap_value'].to_dict()

            title = "Tactical planning horizon:{} degrees\nProportion crossing minimising pedestrians:{}".format(group_key[0], group_key[1])

            # Select the corresponding axis
            ax = axs[pi, qi]

            road_network_subfigure(ax, G, dict_node_pos, dict_edge_values, title, cmap_name = 'viridis', edge_width = 3, edge_alpha = 1)

    f.suptitle(fig_title, fontsize=16, y = 1)
    return f

# Plot crossing counts for each group
'''
dfSub = gdfCrossingCounts.loc[ gdfCrossingCounts['run']==1]
plan_hor = dfSub['tacticalPlanHorizon'].unique()[0]
min_cross = dfSub['minCrossingProp'].unique()[0]
title = "Planning horizon:{}, Min cross prop:{}".format(plan_hor, min_cross)
dict_edge_values = dfSub.set_index('fid')['cmap_value'].to_dict()
single_fig = road_network_figure(G, dict_node_pos, dict_edge_values, title)

path = os.path.join(img_dir, title.replace(":","_")+".png")
single_fig.savefig(path)
single_fig.show()
'''

groupby_columns = ['tacticalPlanHorizon', 'minCrossingProp']
batch_fig = batch_runs_road_network_figure(G, dict_node_pos, gdfCrossingCounts, groupby_columns, "Batch Crossing Counts", cmap_name = 'viridis', edge_width = 3, edge_alpha = 1)

batch_fig_path = os.path.join(img_dir, "figure_"+ os.path.split(ped_crossings_file)[1].replace(".csv", ".png"))
batch_fig.savefig(batch_fig_path)
batch_fig.show()