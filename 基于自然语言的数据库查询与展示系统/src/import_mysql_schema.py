# mysql_schema_quick.py
import json
import mysql.connector
from mysql.connector import Error

def export_schema_quick(config, output_file="schema_quick.json"):
    """快速导出schema（不依赖SQLAlchemy）"""
    try:
        connection = mysql.connector.connect(**config)
        cursor = connection.cursor(dictionary=True)

        # 获取所有表
        cursor.execute(f"SHOW TABLES")
        tables = cursor.fetchall()

        schema = {"tables": []}

        for table_info in tables:
            table_name = table_info[f"Tables_in_{config['database']}"]

            # 获取表注释
            cursor.execute(f"""
                SELECT TABLE_COMMENT
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
            """, (config['database'], table_name))
            table_comment = cursor.fetchone()["TABLE_COMMENT"]

            # 获取字段信息
            cursor.execute(f"DESCRIBE `{table_name}`")
            columns = cursor.fetchall()

            table_data = {
                "name": table_name,
                "comment": table_comment,
                "columns": [
                    {
                        "field": col["Field"],
                        "type": col["Type"],
                        "null": col["Null"] == "YES",
                        "key": col["Key"],
                        "default": col["Default"],
                        "extra": col["Extra"]
                    }
                    for col in columns
                ]
            }

            schema["tables"].append(table_data)

        # 保存为JSON
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(schema, f, indent=2, ensure_ascii=False)

        print(f"Schema已导出到 {output_file}")
        return schema

    except Error as e:
        print(f"数据库连接失败: {e}")
        return None
    finally:
        if connection.is_connected():
            cursor.close()
            connection.close()
       
if __name__ == "__main__":
    cfg = {
        "host": "127.0.0.1",   # 或者宿主机的真实 IP，如果 Docker 不在本机
        "port": 3307,          # 关键：宿主映射端口
        "user": "root",
        "password": "root123456",
        "database": "sakila",  # 你要导出的库名
        "charset": "utf8mb4",  # 可选，防止中文乱码
    }
    export_schema_quick(cfg, output_file="sakila_schema.json")