Nova-Agent Architecture Design
Version：v1.0
一个基于 Java + LangChain4j 的 Agent Runtime 平台，支持 Workflow、ReAct Agent、Skill、Tool、Memory、RAG 等能力。

一、设计目标
Nova-Agent 希望解决的问题：
不是：
做一个类似 Dify 的 Workflow 平台
而是：
构建一个统一的 Agent Runtime。
整个系统以后应该支持：
●  Workflow
●  ReAct Agent
●  Tool Calling
●  MCP
●  Skill
●  RAG
●  Memory
●  Multi-Agent
●  Human In Loop
●  Scheduler
但是：
这些能力都应该建立在统一 Runtime 上。

二、设计原则
Principle 1：Everything is Resource
整个系统里面：
Tool

Workflow

Agent

Skill

Prompt

Knowledge

Memory

Model
全部都是 Resource。
统一：
interface Resource {

    String id();

    ResourceType type();

    String version();

}
以后：
导入
导出
版本
权限
分享
复制
全部统一。

Principle 2：Everything has Runtime
Resource 自己不执行。
Runtime 才负责执行。
例如：
Workflow
↓
WorkflowRuntime

Agent
↓
AgentRuntime

Tool
↓
ToolRuntime

Skill
↓
SkillRuntime
Resource 永远只是 Definition。
Runtime 才负责执行。

Principle 3：Execution is First Class
所有执行都有 Execution。
WorkflowExecution

AgentExecution

ToolExecution

SkillExecution
统一：
interface Execution {

    String executionId();

    ExecutionStatus status();

    Context context();

}
以后：
恢复
暂停
Trace
Retry
都围绕 Execution。

Principle 4：Context is Shared
整个 Runtime 永远只有一个 Context。
不要：
Workflow 自己一个 Map

Agent 一个 Map

Tool 一个 Map
应该：
Context

↓

Workflow

↓

Agent

↓

Tool
所有资源共享 Context。

三、总体架构
REST / SDK
│
▼
Agent Runtime API
│
────────────────────────────────────────────
Runtime Layer
────────────────────────────────────────────

AgentRuntime

WorkflowRuntime

SkillRuntime

ToolRuntime

KnowledgeRuntime

MemoryRuntime

ModelRuntime

────────────────────────────────────────────
Infrastructure
────────────────────────────────────────────

LangChain4j

MySQL

Redis

Milvus

ElasticSearch

Kafka

MinIO

四、核心对象
整个系统只有四个真正的核心对象。
Resource

Runtime

Execution

Context
其它所有东西都建立在它们之上。

1）Resource
所有能力都是 Resource。
Resource
│
├── Tool
├── Workflow
├── Agent
├── Skill
├── Prompt
├── Knowledge
└── Model
统一：
interface Resource {

    String id();

    ResourceType type();

}

2）Runtime
每一种 Resource 都对应 Runtime。
WorkflowRuntime

AgentRuntime

ToolRuntime

SkillRuntime
统一：
interface Runtime {

    Execution execute(
            Resource resource,
            Context context
    );

}

3）Execution
所有执行都有生命周期。
CREATED

RUNNING

WAITING

FAILED

SUCCESS

CANCELLED
Execution 保存：
变量

日志

Trace

Observation

结果

4）Context
整个系统共享 Context。
建议：
class Context {

    Conversation conversation;

    VariableManager variables;

    Memory memory;

    Observation observation;

    RuntimeInfo runtime;

}
Context 永远贯穿整个调用链。

五、Runtime 设计
AgentRuntime
这是整个系统的大脑。
负责：
●  Planner
●  ReAct
●  Tool Calling
●  Observation
●  Memory
●  Decision
执行流程：
User

↓

Planner

↓

Action

↓

Dispatcher

↓

Observation

↓

Planner

↓

Finish
内部：
AgentRuntime

├── Planner

├── Dispatcher

├── ObservationManager

├── MemoryManager

├── ContextManager

└── TraceManager

WorkflowRuntime
负责 DAG。
Workflow

↓

NodeExecutor

↓

Next Node

↓

End
Workflow 不负责思考。
只负责：
按照 DAG 执行。

ToolRuntime
Tool 永远是最小能力。
例如：
HTTP

Java

MCP

Python

Shell
统一：
ToolExecutor

↓

Validate

↓

Invoke

↓

Trace

↓

Return

SkillRuntime
Skill 不负责执行。
Skill 是一种组合。
例如：
Skill

↓

Workflow

↓

Agent

↓

Tool
SkillRuntime：
负责：
解析 Skill。
决定：
调用哪个 Runtime。

六、Skill 设计
Skill 是：
一个可复用能力。
例如：
Java Analyze Skill

SQL Optimize Skill

CRM Skill

Git Skill
Planner：
只看到：
Java Analyze Skill
不会关心：
里面：
到底：
●  Workflow
●  Agent
●  Tool

Skill Definition：
class SkillDefinition {

    String id;

    String name;

    ExecutorType executorType;

    JsonNode config;

}
executorType：
WORKFLOW

AGENT

PROMPT
以后：
支持：
Multi-Agent。

七、Workflow 设计
Workflow：
只关心：
DAG。
Definition：
Workflow

↓

Node

↓

Edge
Node：
LLM

Tool

Agent

If

Loop

Code

End
每一个节点：
都有：
Definition。
执行：
交给：
NodeExecutor。

八、Agent 设计
Agent：
描述：
Model

Prompt

Planner

Memory

Knowledge

Skills

Tools
Definition：
class AgentDefinition {

    String model;

    String systemPrompt;

    Planner planner;

    List<String> skills;

}
真正执行：
AgentRuntime

九、Tool 设计
Tool：
统一：
interface Tool {

    ToolResult invoke(
            JsonNode args
    );

}
Tool Runtime：
支持：
Java Tool

HTTP Tool

MCP Tool

Python Tool

Workflow Tool
以后：
Workflow：
也可以作为 Tool。

十、变量系统
不要：
Map<String,Object>
建议：
class Variable {

    String name;

    VariableType type;

    Object value;

}
Context：
统一：
VariableManager

↓

Variable
以后：
支持：
String

Integer

Boolean

Array

Object

Image

File

LLMMessage

ToolResult

十一、Memory
Memory：
分两种。
Short Memory
History

Summary

Recent Messages

Long Memory
建议：
Fact

Preference

Experience

Skill
以后：
可以：
Embedding。
也可以：
Markdown。

十二、Knowledge
统一：
KnowledgeBase

↓

Document

↓

Chunk

↓

Embedding

↓

Retrieve

↓

Rerank
Knowledge：
通过：
KnowledgeRuntime。

十三、Execution 生命周期
所有 Runtime：
统一：
CREATE

↓

RUNNING

↓

WAITING

↓

SUCCESS

↓

FAILED
以后：
支持：
暂停

恢复

人工介入

Checkpoint

十四、模块划分
nova-agent

├── nova-agent-api
│
├── nova-agent-core
│
├── nova-agent-runtime
│
├── nova-agent-agent
│
├── nova-agent-workflow
│
├── nova-agent-tool
│
├── nova-agent-skill
│
├── nova-agent-memory
│
├── nova-agent-rag
│
├── nova-agent-model
│
├── nova-agent-context
│
├── nova-agent-common
│
└── examples

十五、后续演进路线
V1
完成：
●  Runtime
●  Workflow
●  Tool
●  Agent(ReAct)
这是 MVP。

V2
增加：
●  Skill
●  Memory
●  RAG
●  Prompt

V3
增加：
●  Multi-Agent
●  Supervisor
●  Team

V4
增加：
●  Human In Loop
●  Scheduler
●  Plugin
●  Marketplace

十六、我建议再做一个调整（也是我认为最关键的一点）
经过前面的讨论，我建议不要把 Runtime 设计成多个互相独立的入口，而是采用"统一调度 + 专用执行器"的模式。
也就是说，整个系统实际上只有一个真正的执行入口：
AgentRuntime
│
ExecutorDispatcher
│
┌───────────┬───────────┬───────────┐
▼           ▼           ▼           ▼
Workflow     ToolRuntime  SkillRuntime AgentRuntime
Runtime                                 (递归)
其中：
● AgentRuntime：唯一的调度中心，负责 Planner、ReAct、Context、Memory、Observation。
● WorkflowRuntime：负责执行 DAG，不做自主规划。
● ToolRuntime：负责执行原子能力。
● SkillRuntime：负责解析 Skill，决定调用 Workflow、Agent 或 Tool。
● AgentRuntime（递归）：允许一个 Agent 或 Skill 再次调用另一个 Agent，实现 AutoAgent、Supervisor、Multi-Agent 等高级能力。
这样设计有几个明显优势：
1. 只有一个统一入口，调用链简单，Context 和 Trace 能自然贯穿整个系统。
2. Skill 不需要关心执行细节，只是组合资源，真正执行仍然交给各自的 Runtime。
3. 未来扩展成本极低，增加新的 Skill 类型、Agent 类型甚至新的 Runtime，都不会影响现有架构。
4. Workflow 与 Agent 不再割裂：Workflow 可以调用 Agent，Agent 可以调用 Workflow，二者只是不同的 Resource，而不是两个平行世界。

我认为 nova-agent 最终的核心理念应该只有一句话：
Everything is Resource，Everything runs in Runtime，Everything shares Context，Everything produces Execution。
围绕这四个核心概念（Resource、Runtime、Context、Execution）构建底层，你后续无论增加 Workflow、ReAct、AutoAgent、Multi-Agent、MCP 还是 Skill，都不需要推翻已有设计，只是在统一 Runtime 上增加新的资源类型和执行器即可。

TODO
考虑到真实的使用场景， 应该是前端用户通过各种配置， 生成了一个 tool ， workflow， 最后后端是一个 json， 
数据库也需要保存下来，真实执行，就是从数据库里面去取值，然后跑这个 json 的流程。
