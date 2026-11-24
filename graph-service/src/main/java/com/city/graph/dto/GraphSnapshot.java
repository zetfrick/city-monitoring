package com.city.graph.dto;

import com.city.graph.entity.GraphNode;

import java.util.List;

public record GraphSnapshot(List<GraphNode> nodes, List<GraphLink> edges) {}
