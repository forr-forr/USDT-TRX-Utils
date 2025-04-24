package com.itheima.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.bitcoinj.core.Base58;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.contract.Contract;
import org.tron.trident.core.contract.Trc20Contract;
import org.tron.trident.proto.Response;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionFeeImpl implements TransactionFeeEstimator {
    @Value("${tron.PrivateKey}")
    private String PrivateKey;
    private static final long SUN_PER_USDT = 1_000_000L; // 1 USDT = 1,000,000 Sun
    private static final String API_URL = "https://api.trongrid.io/wallet/triggerconstantcontract";
    private static final String USDT_CONTRACT_ADDRESS = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";
    private static final String TRANSACTION_API_URL = "https://api.trongrid.io/v1/accounts/";


    /**
     *   预估TRX 需要自己实现 主要是为了预计trx
     * @param sender    发送方地址 (Base58 格式)
     * @param recipient 接收方地址 (Base58 格式)
     * @param amount    转账的 TRX 金额
     * @return
     */
    @Override
    public String TRX_TransactionFee(String sender, String recipient, double amount) {
        return null;
    }

    /**
     * @param sender    发送方地址 (Base58 格式)
     * @param recipient 接收方地址 (Base58 格式)
     * @param amount    转账的 USDT 金额
     * @return
     */
    @Override
    public String USDT_TransactionFee(String sender, String recipient, double amount) {
        ApiWrapper client = ApiWrapper.ofMainnet(PrivateKey);
        try {
            // 初始化 USDT 合约（主网 USDT 合约地址）
            Contract contract = client.getContract("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
            Trc20Contract token = new Trc20Contract(contract, sender, client);
            BigInteger usdtBalance = token.balanceOf(sender);
            BigDecimal usdtAmount = new BigDecimal(usdtBalance)
                    .divide(BigDecimal.valueOf(SUN_PER_USDT), 6, RoundingMode.HALF_UP);



            // 获取发送方账户资源
            Response.AccountResourceMessage accountResource = client.getAccountResource(sender);
            long freeNetLimit = accountResource.getFreeNetLimit();
            long freeNetUsed = accountResource.getFreeNetUsed();
            long remainingNet = freeNetLimit - freeNetUsed;
            long energyLimit = accountResource.getEnergyLimit();

            // 获取发送者的 TRX 余额
            Response.Account account = client.getAccount(sender);
            long trxBalanceSun = account.getBalance();
            BigDecimal trxBalance = BigDecimal.valueOf(trxBalanceSun)
                    .divide(BigDecimal.valueOf(SUN_PER_USDT), 6, RoundingMode.HALF_UP);
            double senderTrx = trxBalance.doubleValue();

            boolean senderis = !client.getAccount(sender).getAddress().isEmpty();

            // 发送者 USDT 余额
            double senderUsdt = usdtAmount.doubleValue();
            System.out.println("=========================================");
            System.out.println("🔹 发送者账户资源查询：" + sender);
            System.out.println("=========================================");
            System.out.println("发送者地址是否激活：" + senderis);
            System.out.println("发送者 USDT 余额: " + senderUsdt);
            System.out.println("发送者 TRX 余额: " + senderTrx);
            System.out.println("剩余免费带宽: " + remainingNet);
            System.out.println("账户能量: " + energyLimit);
            System.out.println("=========================================");

            // 判断接收者账户信息
            boolean isRecipientActive = !client.getAccount(recipient).getAddress().isEmpty();
            System.out.println("接收者地址是否激活：" + isRecipientActive);
            BigInteger recipientUsdtBalance = token.balanceOf(recipient);
            BigDecimal recipientUsdtAmount = new BigDecimal(recipientUsdtBalance)
                    .divide(BigDecimal.valueOf(SUN_PER_USDT), 6, RoundingMode.HALF_UP);
            System.out.println("接收者 USDT 余额：" + recipientUsdtAmount);

            // 初始化费用
            BigDecimal bandwidthCostInTrx = BigDecimal.ZERO;
            BigDecimal energyCostInTrx = BigDecimal.ZERO;
            long finalBandwidthUsed = 0; // 实际使用的免费带宽
            long bandwidthToBurn = 0;   // 需要燃烧 TRX 兑换的带宽
            long finalEnergyUsed = 0;   // 需要燃烧 TRX 兑换的能量
            BigDecimal totalTRXCost = BigDecimal.ZERO;


// 估算能量消耗
            long usdtSunAmount = (long) (amount * SUN_PER_USDT);
            int energyCost = estimateEnergyCost(
                    base58ToHex("TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"),
                    base58ToHex(sender),
                    base58ToHex(recipient),
                    usdtSunAmount
            );
            if (energyCost == -1) {
                return "{\"error\":\"能量估算失败\"}";
            }

// 计算实际消耗的账户能量和需要燃烧的能量
            long consumedEnergy = Math.min(energyCost, energyLimit); // 实际从账户能量中消耗的能量
            finalEnergyUsed = Math.max(0, energyCost - energyLimit); // 直接赋值，移除重复的 'long' 声明
            energyCostInTrx = new BigDecimal(finalEnergyUsed * 0.00021)
                    .setScale(6, RoundingMode.HALF_UP); // 1 能量 = 0.00021 TRX

// 处理转账的带宽（USDT 转账约 345 字节）
            long transferBandwidth = 345;
            if (remainingNet >= transferBandwidth) {
                finalBandwidthUsed = transferBandwidth; // 使用免费带宽
                bandwidthToBurn = 0;                   // 无需燃烧 TRX
                bandwidthCostInTrx = BigDecimal.ZERO;
            } else {
                finalBandwidthUsed = 0;                // 不使用免费带宽
                bandwidthToBurn = transferBandwidth;   // 燃烧 TRX 兑换带宽
                bandwidthCostInTrx = new BigDecimal(transferBandwidth * 0.001)
                        .setScale(6, RoundingMode.HALF_UP); // 燃烧 0.345 TRX
            }

// 计算总 TRX 费用
            totalTRXCost = energyCostInTrx.add(bandwidthCostInTrx).setScale(6, RoundingMode.HALF_UP);

// 输出日志
            System.out.println("----------------------------------------------");
            System.out.println("=========================================");
            System.out.println("🔹 TRX 费用明细");
            System.out.println("=========================================");
            System.out.printf("🔸 免费带宽消耗:  %d 带宽\n", finalBandwidthUsed);
            System.out.printf("🔸 带宽需燃烧:    %d 带宽\n", bandwidthToBurn);
            System.out.printf("🔸 带宽费用:      %.6f TRX\n", bandwidthCostInTrx.doubleValue());
            System.out.printf("🔸 账户能量消耗:  %d 能量\n", consumedEnergy);
            System.out.printf("🔸 需燃烧能量:    %d 能量\n", finalEnergyUsed);
            System.out.printf("🔸 能量费用:      %.6f TRX\n", energyCostInTrx.doubleValue());
            System.out.printf("🔸 总 TRX 消耗:   %.6f TRX\n", totalTRXCost.doubleValue());
            System.out.println("=========================================");

// 构造 JSON 结果
            Map<String, Object> result = new HashMap<>();
            result.put("bandwidth_cost", finalBandwidthUsed); // 免费带宽消耗，0 表示未使用免费带宽
            result.put("total_TRX", totalTRXCost.doubleValue()); // 总 TRX 消耗
            result.put("canTransfer", senderTrx >= totalTRXCost.doubleValue()); // 是否可以转账
            result.put("energy_cost", consumedEnergy); // 实际从账户能量扣除的部分

// 带宽费用明细
            Map<String, Object> bandwidthCostMap = new HashMap<>();
            bandwidthCostMap.put("type", "bandwidth");
            bandwidthCostMap.put("deducted", bandwidthToBurn); // 需要燃烧的带宽量
            bandwidthCostMap.put("equivalent_TRX", bandwidthCostInTrx.doubleValue()); // 带宽费用

// 能量费用明细
            Map<String, Object> energyCostMap = new HashMap<>();
            energyCostMap.put("type", "energy");
            energyCostMap.put("deducted", finalEnergyUsed); // 需要燃烧的能量
            energyCostMap.put("equivalent_TRX", energyCostInTrx.doubleValue()); // 能量费用

// 构造 trx_cost 数组
            Map<String, Object>[] trxCost = new Map[]{bandwidthCostMap, energyCostMap};
            result.put("trx_cost", trxCost);

            // 添加发送者信息
            Map<String, Object> senderInfo = new HashMap<>();
            senderInfo.put("address", sender);
            senderInfo.put("isActive", senderis);
            senderInfo.put("usdtBalance", senderUsdt);
            senderInfo.put("trxBalance", senderTrx);
            senderInfo.put("remainingBandwidth", remainingNet);
            senderInfo.put("remainingEnergy", energyLimit);
            result.put("sender", senderInfo);

            // 添加接收者信息
            Map<String, Object> recipientInfo = new HashMap<>();
            recipientInfo.put("address", recipient);
            recipientInfo.put("isActive", isRecipientActive);
            recipientInfo.put("usdtBalance", recipientUsdtAmount.doubleValue());
            result.put("recipient", recipientInfo);

            // 返回 JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"计算手续费失败: " + e.getMessage() + "\"}";
        } finally {
            client.close();
        }
    }

    @Override
    public  String getAddressInfo(String address) {
        ApiWrapper client = ApiWrapper.ofMainnet(PrivateKey);
        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 是否激活
            Response.Account account = client.getAccount(address);
            boolean isActive = account != null && (
                    account.getBalance() > 0 ||
                            account.getAssetV2Count() > 0
            );
            result.put("address", address);

            result.put("isActive", isActive);

            // 2. TRX 余额
            double trxBalance = (account != null)
                    ? account.getBalance() / 1_000_000.0
                    : 0.0;
            result.put("trxBalance", BigDecimal.valueOf(trxBalance)
                    .setScale(6, RoundingMode.HALF_UP).doubleValue());

            // 3. USDT 余额
            Contract contract = client.getContract(USDT_CONTRACT_ADDRESS);
            Trc20Contract token = new Trc20Contract(contract, address, client);
            BigInteger usdtBalanceRaw = token.balanceOf(address);
            double usdtBalance = new BigDecimal(usdtBalanceRaw)
                    .divide(BigDecimal.valueOf(SUN_PER_USDT), 6, RoundingMode.HALF_UP)
                    .doubleValue();
            result.put("usdtBalance", usdtBalance);

            // 4. 带宽
            Response.AccountResourceMessage resource = client.getAccountResource(address);
            Map<String, Long> bandwidth = new HashMap<>();
            bandwidth.put("used", resource != null ? resource.getFreeNetUsed() : 0);
            bandwidth.put("total", resource != null ? resource.getFreeNetLimit() : 0);
            result.put("bandwidth", bandwidth);

            // 5. 能量
            long energy = resource != null ? resource.getEnergyLimit() : 0;
            result.put("energy", energy);

            // 6. 获取最近十笔的USDT收款和发送时间
            // 获取最近十笔的USDT收款和发送时间
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String lastReceiveTime = "无记录"; // 默认无记录
                String lastSendTime = "无记录"; // 默认无记录

                // 查询最近十笔 USDT 收款和发送
                String url = TRANSACTION_API_URL + address + "/transactions/trc20"
                        + "?limit=10&only_confirmed=true&contract_address=" + USDT_CONTRACT_ADDRESS
                        + "&to=" + address + "&from=" + address;

                System.out.println(url);
                HttpGet request = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    JsonNode node = objectMapper.readTree(response.getEntity().getContent());
                    JsonNode transactions = node.get("data");

                    if (transactions != null && transactions.isArray()) {
                        for (JsonNode tx : transactions) {
                            String to = tx.get("to").asText().toLowerCase();
                            String from = tx.get("from").asText().toLowerCase();

                            // 如果是收款
                            if (to.equals(address.toLowerCase()) && lastReceiveTime.equals("无记录")) {
                                long timestamp = tx.get("block_timestamp").asLong();
                                lastReceiveTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
                            }

                            // 如果是发送
                            if (from.equals(address.toLowerCase()) && lastSendTime.equals("无记录")) {
                                long timestamp = tx.get("block_timestamp").asLong();
                                lastSendTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
                            }

                            // 如果找到了收款和发送时间都不为空，就可以跳出
                            if (!lastReceiveTime.equals("无记录") && !lastSendTime.equals("无记录")) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                result.put("lastUsdtReceiveTime", lastReceiveTime);
                result.put("lastUsdtSendTime", lastSendTime);
            }

            // 7. 地址创建时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String createTime = (account != null && account.getCreateTime() > 0)
                    ? sdf.format(new Date(account.getCreateTime()))
                    : "未激活或无记录";
            result.put("createTime", createTime);

            // 返回 JSON
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"查询失败: " + e.getMessage() + "\"}";
        } finally {
            client.close();
        }
    }

    /**
     * 估算能量消耗
     */
    private int estimateEnergyCost(String contractAddress, String ownerAddress, String recipientAddress, long amountSun) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL);
            request.setHeader("Content-Type", "application/json");

            String encodedRecipient = "000000000000000000000000" + recipientAddress.substring(2);
            String encodedAmount = String.format("%064x", amountSun);

            String jsonBody = String.format(
                    "{ \"contract_address\": \"%s\", " +
                            "\"owner_address\": \"%s\", " +
                            "\"function_selector\": \"transfer(address,uint256)\", " +
                            "\"parameter\": \"%s%s\", " +
                            "\"call_value\": 0 }",
                    contractAddress, ownerAddress, encodedRecipient, encodedAmount
            );
            request.setEntity(new StringEntity(jsonBody));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());

                if (jsonResponse.has("energy_used")) {
                    return jsonResponse.get("energy_used").asInt();
                } else {
                    System.err.println("API 未返回 energy_used 字段");
                    return -1;
                }
            }
        } catch (Exception e) {
            System.err.println("能量估算失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 将 Base58 地址转换为 Hex 格式
     */
    private String base58ToHex(String base58Address) {
        if (base58Address == null || base58Address.isEmpty()) {
            throw new IllegalArgumentException("地址不能为空");
        }
        try {
            byte[] decoded = Base58.decode(base58Address);
            if (decoded.length < 21) {
                throw new IllegalArgumentException("无效的 TRON 地址");
            }
            byte[] addressBytes = new byte[21];
            System.arraycopy(decoded, 0, addressBytes, 0, 21);
            return Hex.toHexString(addressBytes);
        } catch (Exception e) {
            throw new RuntimeException("地址转换失败: " + e.getMessage(), e);
        }
    }
}