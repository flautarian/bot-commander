package com.giacconidev.balancer.backend.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Task {
    private String id;
    private String actionType;
    private Map<String, String> parameters = new HashMap<String,String>();
    private String result;
}
