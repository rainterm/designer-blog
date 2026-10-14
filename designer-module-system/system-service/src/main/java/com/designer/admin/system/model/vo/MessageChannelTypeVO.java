package com.designer.admin.system.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageChannelTypeVO {

    private String type;
    private String label;
    private List<ConfigFieldMeta> fields;
}
