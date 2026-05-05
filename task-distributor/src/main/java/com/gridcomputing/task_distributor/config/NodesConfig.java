package com.gridcomputing.task_distributor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "")
public class NodesConfig {

    private List<Node> nodes = new ArrayList<>();

    @Data
    public static class Node {
        private String id;
        private String host;
        private int port;
    }
}
