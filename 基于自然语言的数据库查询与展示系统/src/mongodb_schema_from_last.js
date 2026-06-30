// MongoDB数据库集合和索引创建脚本 - 严格结构定义
// 完全按照数据库设计文档

db = db.getSiblingDB('natural_language_query_system');
print("开始初始化 MongoDB 集合...");

try {
    // 1. query_collections（收藏查询表-组）
    db.createCollection("query_collections", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["userId", "groupName", "createTime"],
                properties: {
                    userId: {
                        bsonType: "long",
                        description: "用户ID，关联users.id"
                    },
                    groupName: {
                        bsonType: "string",
                        description: "收藏组名称（由大模型生成）"
                    },
                    createTime: {
                        bsonType: "date",
                        description: "组创建时间"
                    }
                }
            }
        }
    });
    db.query_collections.createIndex({ "userId": 1, "groupName": 1 }, { unique: true });
    print("创建 query_collections 完成");
} catch(e) {
    print("query_collections 创建错误: " + e);
}

try {
    // 2. collection_records（收藏记录表-具体记录）
    db.createCollection("collection_records", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["queryId", "userId", "sqlContent", "dbConnectionId", "llmConfigId", "createTime"],
                properties: {
                    queryId: {
                        bsonType: "objectId",
                        description: "组ID，关联query_collections._id"
                    },
                    userId: {
                        bsonType: "long",
                        description: "用户ID，关联users.id"
                    },
                    sqlContent: {
                        bsonType: "string",
                        description: "收藏的SQL语句"
                    },
                    queryResult: {
                        bsonType: "object",
                        description: "结果数据"
                    },
                    dbConnectionId: {
                        bsonType: "long",
                        description: "数据库来源，关联db_connections.id"
                    },
                    llmConfigId: {
                        bsonType: "long",
                        description: "大模型来源，关联llm_configs.id"
                    },
                    createTime: {
                        bsonType: "date",
                        description: "收藏时间"
                    }
                }
            }
        }
    });
    db.collection_records.createIndex({ "queryId": 1, "userId": 1 });
    db.collection_records.createIndex({ "dbConnectionId": 1 });
    db.collection_records.createIndex({ "llmConfigId": 1 });
    print("创建 collection_records 完成");
} catch(e) {
    print("collection_records 创建错误: " + e);
}

try {
    // 3. dialog_records（多轮对话列表）
    db.createCollection("dialog_records", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["dialogId", "userId", "topic", "totalRounds", "startTime", "lastTime"],
                properties: {
                    dialogId: {
                        bsonType: "string",
                        description: "对话ID，自定义唯一标识"
                    },
                    userId: {
                        bsonType: "long",
                        description: "用户ID，关联users.id"
                    },
                    topic: {
                        bsonType: "string",
                        description: "对话主题（大模型生成）"
                    },
                    totalRounds: {
                        bsonType: "int",
                        description: "对话总轮次",
                        minimum: 0
                    },
                    startTime: {
                        bsonType: "date",
                        description: "对话开始时间"
                    },
                    lastTime: {
                        bsonType: "date",
                        description: "最后一轮对话时间"
                    }
                }
            }
        }
    });
    db.dialog_records.createIndex({ "userId": 1, "lastTime": -1 });
    db.dialog_records.createIndex({ "dialogId": 1 }, { unique: true });
    print("创建 dialog_records 完成");
} catch(e) {
    print("dialog_records 创建错误: " + e);
}

try {
    // 4. dialog_details（多轮对话具体内容表）
    db.createCollection("dialog_details", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["dialogId", "rounds"],
                properties: {
                    dialogId: {
                        bsonType: "string",
                        description: "对话ID，关联dialog_records.dialogId"
                    },
                    rounds: {
                        bsonType: "array",
                        description: "对话轮次数组",
                        items: {
                            bsonType: "object",
                            required: ["roundNum", "userInput", "aiResponse", "generatedSql", "roundTime"],
                            properties: {
                                roundNum: {
                                    bsonType: "int",
                                    description: "轮次序号",
                                    minimum: 1
                                },
                                userInput: {
                                    bsonType: "string",
                                    description: "用户输入"
                                },
                                aiResponse: {
                                    bsonType: "string",
                                    description: "AI回复"
                                },
                                generatedSql: {
                                    bsonType: "string",
                                    description: "生成的SQL"
                                },
                                roundTime: {
                                    bsonType: "date",
                                    description: "轮次时间"
                                }
                            }
                        }
                    }
                }
            }
        }
    });
    db.dialog_details.createIndex({ "dialogId": 1 });
    print("创建 dialog_details 完成");
} catch(e) {
    print("dialog_details 创建错误: " + e);
}

try {
    // 5. sql_cache（SQL缓存集合）
    db.createCollection("sql_cache", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["nlHash", "connectionId", "tableIds", "dbType", "generatedSql", "hitCount", "expireTime"],
                properties: {
                    nlHash: {
                        bsonType: "string",
                        description: "自然语言查询的MD5哈希"
                    },
                    userId: {
                        bsonType: ["long", "null"],
                        description: "用户ID（NULL为全局缓存）"
                    },
                    connectionId: {
                        bsonType: "long",
                        description: "数据源ID，关联db_connections.id"
                    },
                    tableIds: {
                        bsonType: "array",
                        description: "涉及的表ID列表",
                        items: {
                            bsonType: "long"
                        }
                    },
                    dbType: {
                        bsonType: "string",
                        description: "数据库类型"
                    },
                    generatedSql: {
                        bsonType: "string",
                        description: "缓存的SQL语句"
                    },
                    hitCount: {
                        bsonType: "int",
                        description: "命中次数",
                        minimum: 0
                    },
                    expireTime: {
                        bsonType: "date",
                        description: "缓存过期时间"
                    }
                }
            }
        }
    });
    db.sql_cache.createIndex({ "nlHash": 1, "connectionId": 1, "tableIds": 1 });
    db.sql_cache.createIndex({ "expireTime": 1 }, { expireAfterSeconds: 0 });
    print("创建 sql_cache 完成");
} catch(e) {
    print("sql_cache 创建错误: " + e);
}

try {
    // 6. ai_interaction_logs（AI交互日志集合）
    db.createCollection("ai_interaction_logs", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["userId", "requestType", "llmName", "requestParams", "tokenUsage", "responseTime", "status", "createTime"],
                properties: {
                    userId: {
                        bsonType: "long",
                        description: "用户ID，关联users.id"
                    },
                    requestType: {
                        bsonType: "string",
                        enum: ["nl2sql", "sql_optimize"],
                        description: "请求类型"
                    },
                    llmName: {
                        bsonType: "string",
                        description: "模型名称，关联llm_configs.name"
                    },
                    requestParams: {
                        bsonType: "object",
                        description: "请求参数",
                        properties: {
                            naturalLanguage: {
                                bsonType: "string",
                                description: "自然语言查询"
                            },
                            metadata: {
                                bsonType: "object",
                                description: "元数据信息"
                            },
                            temperature: {
                                bsonType: "double",
                                description: "温度参数",
                                minimum: 0,
                                maximum: 2
                            }
                        }
                    },
                    responseResult: {
                        bsonType: "object",
                        description: "响应结果",
                        properties: {
                            sql: {
                                bsonType: "string",
                                description: "生成的SQL"
                            },
                            confidence: {
                                bsonType: "double",
                                description: "置信度",
                                minimum: 0,
                                maximum: 1
                            },
                            suggestion: {
                                bsonType: "string",
                                description: "优化建议"
                            }
                        }
                    },
                    tokenUsage: {
                        bsonType: "object",
                        description: "Token消耗",
                        required: ["promptTokens", "completionTokens", "totalTokens"],
                        properties: {
                            promptTokens: {
                                bsonType: "int",
                                description: "输入token数",
                                minimum: 0
                            },
                            completionTokens: {
                                bsonType: "int",
                                description: "输出token数",
                                minimum: 0
                            },
                            totalTokens: {
                                bsonType: "int",
                                description: "总token数",
                                minimum: 0
                            }
                        }
                    },
                    responseTime: {
                        bsonType: "int",
                        description: "响应耗时（ms）",
                        minimum: 0
                    },
                    status: {
                        bsonType: "string",
                        enum: ["success", "fail"],
                        description: "交互状态"
                    },
                    errorMsg: {
                        bsonType: ["string", "null"],
                        description: "错误信息"
                    },
                    createTime: {
                        bsonType: "date",
                        description: "请求发起时间"
                    }
                }
            }
        }
    });
    db.ai_interaction_logs.createIndex({ "userId": 1, "createTime": -1 });
    db.ai_interaction_logs.createIndex({ "llmName": 1, "status": 1 });
    print("创建 ai_interaction_logs 完成");
} catch(e) {
    print("ai_interaction_logs 创建错误: " + e);
}

try {
    // 7. friend_chats（好友聊天记录表）- 完全按照设计文档
    db.createCollection("friend_chats", {
        validator: {
            $jsonSchema: {
                bsonType: "object",
                required: ["user_id", "friend_id", "content_type", "content", "send_time", "is_read"],
                properties: {
                    user_id: {
                        bsonType: "long",
                        description: "发送人ID，关联users.id"
                    },
                    friend_id: {
                        bsonType: "long",
                        description: "接收人ID，关联users.id"
                    },
                    content_type: {
                        bsonType: "string",
                        enum: ["text", "query_share", "image"],
                        description: "消息类型"
                    },
                    content: {
                        bsonType: "object",
                        description: "动态消息内容",
                        properties: {
                            text: {
                                bsonType: "string",
                                description: "文本内容"
                            },
                            query_id: {
                                bsonType: "string",
                                description: "查询ID"
                            },
                            title: {
                                bsonType: "string",
                                description: "查询标题"
                            },
                            url: {
                                bsonType: "string",
                                description: "图片URL"
                            },
                            size: {
                                bsonType: "int",
                                description: "文件大小",
                                minimum: 0
                            }
                        }
                    },
                    send_time: {
                        bsonType: "date",
                        description: "消息发送时间"
                    },
                    is_read: {
                        bsonType: "bool",
                        description: "是否已读"
                    },
                    extra: {
                        bsonType: "object",
                        description: "额外信息（扩展字段）",
                        properties: {
                            quote_msg_id: {
                                bsonType: "objectId",
                                description: "引用消息ID"
                            },
                            is_recalled: {
                                bsonType: "bool",
                                description: "是否撤回"
                            }
                        }
                    }
                }
            }
        }
    });
    db.friend_chats.createIndex({ "user_id": 1, "friend_id": 1, "send_time": -1 });
    db.friend_chats.createIndex({ "friend_id": 1, "is_read": 1 });
    print("创建 friend_chats 完成");
} catch(e) {
    print("friend_chats 创建错误: " + e);
}

// 验证创建结果
var collections = db.getCollectionNames();
var userCollections = collections.filter(function(name) {
    return !name.startsWith('system.');
});

print("\n==========================================");
print("MongoDB 集合初始化完成");
print("用户集合数量: " + userCollections.length);
print("集合列表: " + JSON.stringify(userCollections.sort()));
print("==========================================");

// 检查是否所有集合都创建成功
var expectedCollections = [
    "query_collections",
    "collection_records",
    "dialog_records",
    "dialog_details",
    "sql_cache",
    "ai_interaction_logs",
    "friend_chats"
].sort();

var missingCollections = expectedCollections.filter(function(name) {
    return userCollections.indexOf(name) === -1;
});

if (missingCollections.length > 0) {
    print("缺失的集合: " + JSON.stringify(missingCollections));
} else {
    print("所有集合创建成功！");

    // 显示各集合的索引信息
    print("\n📈 各集合索引详情:");
    userCollections.forEach(function(collectionName) {
        var indexes = db[collectionName].getIndexes();
        var userIndexes = indexes.filter(function(index) {
            return index.name !== "_id_";
        });
        print("  " + collectionName + ": " + userIndexes.length + " 个索引");
        userIndexes.forEach(function(index) {
            print("    - " + index.name + ": " + JSON.stringify(index.key));
        });
    });
}

print("\n集合结构验证:");
userCollections.forEach(function(collectionName) {
    var stats = db[collectionName].stats();
    print("  " + collectionName + ": " + stats.count + " 个文档, " + stats.size + " 字节");
});

print("\nMongoDB 严格结构定义初始化完成！");

// ========================================
// 插入初始示例数据（可选）
// ========================================
print("\n开始插入初始示例数据...");

// 注意：MongoDB 的初始数据通常不需要预先插入，
// 因为应用运行时会自动创建文档。
// 这里仅作为演示，实际项目中可以删除或保留为空。

print("初始数据插入完成（当前为空，应用运行时自动生成）");