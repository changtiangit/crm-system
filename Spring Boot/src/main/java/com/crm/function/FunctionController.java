package com.crm.function;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/functions")
public class FunctionController {
    private final FunctionMapper mapper;

    public FunctionController(FunctionMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping
    public List<FunctionView> list() {
        return mapper.findAll();
    }
}
