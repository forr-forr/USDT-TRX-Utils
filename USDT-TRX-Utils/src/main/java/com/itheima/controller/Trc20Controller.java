package com.itheima.controller;

import cn.dev33.satoken.util.SaResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.service.TransactionFeeEstimator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/wallets")
public class Trc20Controller {

    @Autowired
    TransactionFeeEstimator TransactionFeeImpl;

    /**
     * 处理交易费用查询请求
     * @param requestBody 包含发送者地址、接收者地址、金额和转账类型的请求体
     * @return 查询结果，成功返回费用数据，失败返回错误信息
     */
    @PostMapping("/gettransaction")
    public SaResult submitTransaction(@RequestBody Map<String, Object> requestBody) {
        // 获取请求参数
        String senderAddress = (String) requestBody.get("senderAddress");
        String receiverAddress = (String) requestBody.get("receiverAddress");
        String amountStr = requestBody.get("amount") != null ? requestBody.get("amount").toString() : null;
        String transferType = (String) requestBody.get("transferType");

        // 验证参数完整性
        if (StringUtils.isBlank(senderAddress) || StringUtils.isBlank(receiverAddress) ||
                StringUtils.isBlank(amountStr) || StringUtils.isBlank(transferType)) {
            return SaResult.error("交易信息不完整");
        }

        // 验证金额格式
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                return SaResult.error("金额必须大于 0");
            }
        } catch (NumberFormatException e) {
            return SaResult.error("金额格式错误");
        }

        // 验证转账类型
        if (!"USDT".equalsIgnoreCase(transferType) && !"TRX".equalsIgnoreCase(transferType)) {
            return SaResult.error("仅支持 USDT 或 TRX 转账");
        }

        // 构造交易信息
        Map<String, Object> transactionInfo = new HashMap<>();
        transactionInfo.put("senderAddress", senderAddress);
        transactionInfo.put("receiverAddress", receiverAddress);
        transactionInfo.put("amount", amount);
        transactionInfo.put("transferType", transferType);

        // 根据转账类型调用相应的费用估算方法
        String jsonString;
        if (transferType.equalsIgnoreCase("USDT")) {
            jsonString = TransactionFeeImpl.USDT_TransactionFee(senderAddress, receiverAddress, amount);
        } else {
            jsonString = TransactionFeeImpl.TRX_TransactionFee(senderAddress, receiverAddress, amount);
        }

        // 检查费用估算结果是否有效
        if (StringUtils.isBlank(jsonString)) {
            return SaResult.error("手续费查询失败：无法获取交易费用信息");
        }

        // 解析 JSON 数据
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonData;
        try {
            jsonData = objectMapper.readValue(jsonString, Map.class);
        } catch (JsonProcessingException e) {
            // 记录错误日志便于调试
            System.err.println("JSON 解析错误，transferType: " + transferType + "，jsonString: " + jsonString);
            return SaResult.error("查询失败：数据解析错误");
        }

        // 返回成功结果
        return SaResult.ok().setData(jsonData);
    }


    @PostMapping("/getaddressstatus")
    public SaResult getaddressstatus(@RequestBody Map<String, Object> requestBody) {
        // 获取地址列表
        List<String> addresses = (List<String>) requestBody.get("addresses");
        if (addresses == null || addresses.isEmpty()) {
            return SaResult.error("地址列表不能为空");
        }

        // 验证地址格式
        for (String address : addresses) {
            if (StringUtils.isBlank(address) || !address.startsWith("T") || address.length() != 34) {
                return SaResult.error("无效的 TRC20 地址: " + address);
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 遍历地址并获取信息
        for (String address : addresses) {
            String jsonString = TransactionFeeImpl.getAddressInfo(address);
            if (StringUtils.isBlank(jsonString)) {
                return SaResult.error("无法获取地址信息: " + address);
            }

            try {
                // 解析 JSON 字符串为 Map
                Map<String, Object> addressInfo = objectMapper.readValue(jsonString, Map.class);
                resultList.add(addressInfo);
            } catch (JsonProcessingException e) {
                System.err.println("JSON 解析错误，address: " + address + "，jsonString: " + jsonString);
                return SaResult.error("查询失败：数据解析错误");
            }
        }

        // 始终返回数组格式
        return SaResult.ok().setData(resultList);
    }

}