package com.giacconidev.balancer.backend.model;

import java.util.ArrayList;
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
@Document(collection = "bots")
public class Bot {

    @Id
    private String id;
    private String name;
    private String status;
    private String os;
    private ArrayList<Task> tasks = new ArrayList<>();
}
