# WorldGenesis (Spring AI 版)

基于 Spring Boot + Spring AI 重写的 WorldGenesis。保留原有功能：从 Neo4j 混合采样剧情上下文，调用 LLM 归纳世界观，输出 `source_world_bible.json`；并提供 JSON -> Neo4j 的导入能力。对外暴露 HTTP 接口。

## 准备环境
- Java 17+，Maven 3.9+。
- 依赖环境变量（与原 Python 版本保持一致）：
  - `NEO4J_URI` / `NEO4J_USER` / `NEO4J_PASSWORD`
  - `OPENAI_API_KEY`（可选 `OPENAI_MODEL`、`OPENAI_BASE_URL`）
  - `WORLD_SAMPLE_SIZE`（默认 40）、`CONTEXT_CHAR_LIMIT`（默认 12000）
  - `WORLD_OUTPUT_FILE`（默认 `source_world_bible.json`）
  - `WORLD_DATA_FILE`（默认 `records-15.json`，用于导入）

## 构建与运行
```bash
mvn package
java -jar target/world-genesis-1.0.0-SNAPSHOT.jar
```

服务启动后可调用接口：
- `POST /api/worldgenesis/generate` 生成世界观  
  请求体可选：`{"sampleSize":50,"output":"world_bible.json"}`，output 为空串则不落盘；缺省使用配置默认值。
- `POST /api/worldgenesis/seed` 导入 JSON 数据到 Neo4j  
  请求体可选：`{"dataFile":"./data/records.json"}`，缺省使用 `WORLD_DATA_FILE`。

如仍需原 CLI 行为，可在启动时加 `--worldgenesis.cli.enabled=true`，参数同旧版：
- `--sample-size`、`--output` 控制生成
- `--seed`、`--seed-file` 导入数据，`--seed-only` 仅导入
- `--skip-generate` 跳过生成
