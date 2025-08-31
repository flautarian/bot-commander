package com.giacconidev.botcommander.backend.model;

import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@Document(collection = "processes")
public class Process {

    @Id
    private String id;
    private String name;
    private String description;
    private String actionType;
    private Map<String, String> parameters = new HashMap<String, String>();
}
