# USDT-TRX-Utils

## 简介

USDT-TRX-Utils 是一个基于 Spring Boot 的 TRON 区块链（TRC20）查询工具，提供 USDT 和 TRX 转账手续费估算以及 TRC20 地址状态查询功能。项目包含 RESTful API 和 Layui 框架的前端界面，用户可通过 Web 界面或直接调用 API 获取费用和地址信息。适用于开发者、区块链爱好者和需要快速查询 TRON 网络信息的用户。

**在线测试地址**: [https://trcfee.com/](https://trcfee.com/)

## 功能

1. **手续费查询**
   - **端点**: `POST /wallets/gettransaction`
   - **功能**: 估算 USDT 或 TRX 转账的带宽、能量和总 TRX 费用。
   - **输入**: 发送者地址、接收者地址（可选）、金额、转账类型（USDT 或 TRX）。
   - **输出**: 费用明细（带宽、能量、总 TRX）、发送者和接收者账户信息（余额、状态、资源）、是否可转账。
   - **前端**: `fee-query.html` 提供交互式表单，卡片式展示结果，包含余额警告和费用提示。


2. **地址状态查询**
   - **端点**: `POST /wallets/getaddressstatus`
   - **功能**: 查询单个或多个 TRC20 地址的详细信息。
   - **输出**: 激活状态、TRX/USDT 余额、激活时间、最后 USDT 发送/接收时间、剩余带宽和能量。
   - **前端**: `address-status.html` 以表格形式展示，支持单地址或批量查询，包含币种图标。




## 技术栈

- **后端**:
  - Spring Boot: RESTful API 框架。
  - Tron Trident: TRON 区块链交互。
  - Jackson: JSON 处理。
  - Apache HttpClient: TRON API 调用。
  - BitcoinJ: Base58 地址解码。
  - Bouncy Castle: Hex 编码。
  - Sa-Token: 响应格式化。
  - Apache Commons Lang3: 字符串处理。
- **前端**:
  - Layui: 响应式 UI 框架。
  - JavaScript: 动态页面加载和 API 交互。
- **配置**: `application.yml`（默认端口 8082）。

## 前置条件

- Java 8 或更高版本
- Maven 3.6+
- Node.js（可选，用于前端开发）

## ⚠️ 关于 USDT 私钥配置的重要说明

在本项目中，配置文件中需要填写的是 **USDT 的私钥地址**，**并非 API 密钥**。

### ❗ 请务必注意以下几点：

- **千万不要使用持有真实资金的钱包私钥！**
- 本项目中的私钥配置 **仅用于查询/测试用途**，不会发起真实交易。
- 建议前往 [TronGrid](https://www.trongrid.io) 或使用 TronLink 等工具，**生成一个空钱包私钥地址**，用于填入配置文件。
- 你也可以从网上随意搜索一个无资金地址的私钥，仅用于本地测试。

### 示例配置（application.yml）：

```yaml
tron:
  private-key: "your-test-private-key-here" # 注意：请勿使用真实钱包的私钥

## 私钥配置（测试用）

⚠️ 注意：以下私钥仅用于测试目的，**切勿**用于主网或真实交易环境。


私钥列表 注意：以下私钥仅用于测试目的，切勿用于主网或真实交易或导入钱包使用 否则会有资金丢失风险
c43eab563a054a2ddf20b7aafd868e5e88e43a2faa08b1f66c03ab26327f1d9d  
fe51bd52493e573fbb9f9b0b71d176fc334c04d1045d7b5774448da41a79c0f0  
080b624e303c2b69af5177599212488605dd40ebf85f490864a22fbd3372be7e  
081033d369cfc372589eb104ced585d801728c1a5731a532af9e99fa16aa8ca1  
bb7d42fa619f90e93dadc3368fc6ca44fc88d5b33b98998455f896aab4a9acd1  
f79f6a1c163982054c437ea90f457d7a8fde2e9b1489b4e4397063e44bde9e85  
03e2112aae3cf4511eec5176e79ed773131f31fef847ac53a7719b0667c84ed4  
809ffc9d5a07ea30435be36cf3b101a00c4e4b9cd93333b8a75e82c246143492  
bb6d70db62b5bcc946c5f6c0b4bbf1a8256392ccbd9e0c6b22f6477e471d4289  
2ee993f04f884f2eeac820ec4376b79af0216dd943c05824751aee42782e8b63  
34f4abc7640fed0fbc3497c5b4cda0194d97a15f1035e3a448ec1c9fb0f2087a  
c7032d5e09b1b7783ce98faac38fababfdc1ca107acd6848e31d45e14f62bbcc  
8470fb3dcc33aeaf819fc754b3e0d639331ba6662f51fb3ca597bdc0f789940f  
