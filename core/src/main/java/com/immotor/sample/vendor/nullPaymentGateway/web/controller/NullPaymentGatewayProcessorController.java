/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.immotor.sample.vendor.nullPaymentGateway.web.controller;

import com.alipay.config.AlipayConfig;
import com.alipay.util.httpClient.AlipaySubmit;
import com.immotor.sample.payment.service.gateway.NullPaymentGatewayConfiguration;
import com.immotor.sample.vendor.nullPaymentGateway.service.payment.NullPaymentGatewayConstants;
import com.wechat.constant.GlobalConfig;
import com.wechat.util.GetWxOrderno;
import com.wechat.util.RequestHandler;
import com.wechat.util.TenpayUtil;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;

/**
 * This is a sample implementation of a Payment Gateway Processor.
 * This mimics what a real payment gateway might do in order to process
 * credit card information. So, this controller will handle the POST
 * from your credit card form on your checkout page and do some
 * minimal Credit Card Validation (luhn check and expiration date is after today).
 * In production, that form should securely POST to your third party payment gateway and not this controller.
 *
 * In order to use this sample controller, you will need to component scan
 * the package "com.immotor.sample".
 *
 * This should NOT be used in production, and is meant solely for demonstration
 * purposes only.
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Controller("blNullPaymentGatewayProcessorController")
public class NullPaymentGatewayProcessorController {

    private static Logger logger = Logger.getLogger(NullPaymentGatewayProcessorController.class.getName());
    @Resource(name = "blNullPaymentGatewayConfiguration")
    protected NullPaymentGatewayConfiguration paymentGatewayConfiguration;

    @Resource(name = "blOrderService")
    protected OrderService orderService;

    /**
     * WeChat pay
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/null-checkout/wechat/process", method = RequestMethod.POST)
    public
    @ResponseBody
    String processWechat(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String[]> paramMap = request.getParameterMap();

        String currTime = TenpayUtil.getCurrTime();
        String strTime = currTime.substring(8, currTime.length());
        String strRandom = TenpayUtil.buildRandom(4) + "";
        String nonce_str = strTime + strRandom;
        String order_price = ""; // 价格
        String body = NullPaymentGatewayConstants.PAY_BODY;   // 商品名称
        String out_trade_no = ""; // order id

        if (paramMap.get(NullPaymentGatewayConstants.ORDER_ID) != null
                && paramMap.get(NullPaymentGatewayConstants.ORDER_ID).length > 0) {
            out_trade_no = paramMap.get(NullPaymentGatewayConstants.ORDER_ID)[0];
        }

        Order order = orderService.findOrderById(Long.parseLong(out_trade_no));
        int fee = order.getTotal().getAmount().multiply(new BigDecimal(100)).intValue();
        order_price = Integer.toString(fee);
        // 获取发起电脑 ip
        String spbill_create_ip = request.getRemoteAddr();
        // 回调接口
        String notify_url = GlobalConfig.return_url + "/config/weixinPay_result";

        String trade_type = "NATIVE";

        SortedMap<String, String> packageParams = new TreeMap<String, String>();
        packageParams.put("appid", GlobalConfig.APPID);
        packageParams.put("mch_id", GlobalConfig.MCH_ID);
        packageParams.put("nonce_str", nonce_str);

        packageParams.put("body", body);
        packageParams.put("out_trade_no", out_trade_no);
        packageParams.put("total_fee", order_price);
        packageParams.put("spbill_create_ip", spbill_create_ip);
        packageParams.put("notify_url", notify_url);
        packageParams.put("trade_type", trade_type);

        RequestHandler requestHandler = new RequestHandler(request, response);
        requestHandler.init(GlobalConfig.APPID, GlobalConfig.APPSECRET, GlobalConfig.KEY);

        String sign = requestHandler.createSign(packageParams);
        String xml = "<xml>" +
                "<appid>" + GlobalConfig.APPID + "</appid>" +
                "<mch_id>" + GlobalConfig.MCH_ID + "</mch_id>" +
                "<nonce_str>" + nonce_str + "</nonce_str>" +
                "<sign>" + sign + "</sign>" +
                "<body><![CDATA[" + body + "]]></body>" +
                "<out_trade_no>" + out_trade_no + "</out_trade_no>" +
                "<total_fee>" + order_price + "</total_fee>" +
                "<spbill_create_ip>" + spbill_create_ip + "</spbill_create_ip>" +
                "<notify_url>" + notify_url + "</notify_url>" +
                "<trade_type>" + trade_type + "</trade_type>" +
                "</xml>";

        String createOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
        String code_url;
        code_url = new GetWxOrderno().getUrlCode(createOrderURL, xml);
        if (code_url.equals("")) {
            logger.info("code_url error");
        }
        return code_url;
    }

    @RequestMapping(value = "/null-checkout/process", method = RequestMethod.POST)
    public @ResponseBody String processTransparentRedirectForm(HttpServletRequest request){
        Map<String,String[]> paramMap = request.getParameterMap();

//        String transactionAmount = "";
        String orderId="";
//        String billingFirstName = "";
//        String billingLastName = "";
//        String billingAddressLine1 = "";
//        String billingAddressLine2 = "";
//        String billingCity = "";
//        String billingState = "";
//        String billingZip = "";
//        String billingCountry = "";
//        String shippingFirstName = "";
//        String shippingLastName = "";
//        String shippingAddressLine1 = "";
//        String shippingAddressLine2 = "";
//        String shippingCity = "";
//        String shippingState = "";
//        String shippingZip = "";
//        String shippingCountry = "";
//        String creditCardName = "";
//        String creditCardNumber = "";
//        String creditCardExpDate = "";
//        String creditCardCVV = "";
        String cardType = "alipay";

        String resultMessage = "";
        String resultSuccess = "true";
        String gatewayTransactionId = UUID.randomUUID().toString();

//        if (paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT) != null
//                && paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT).length > 0) {
//            transactionAmount = paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT)[0];
//        }
//
        if (paramMap.get(NullPaymentGatewayConstants.ORDER_ID) != null
                && paramMap.get(NullPaymentGatewayConstants.ORDER_ID).length > 0) {
            orderId = paramMap.get(NullPaymentGatewayConstants.ORDER_ID)[0];
        }



        StringBuffer response = new StringBuffer();
        response.append("<!DOCTYPE HTML>");
        response.append("<!--[if lt IE 7]> <html class=\"no-js lt-ie9 lt-ie8 lt-ie7\" lang=\"en\"> <![endif]-->");
        response.append("<!--[if IE 7]> <html class=\"no-js lt-ie9 lt-ie8\" lang=\"en\"> <![endif]-->");
        response.append("<!--[if IE 8]> <html class=\"no-js lt-ie9\" lang=\"en\"> <![endif]-->");
        response.append("<!--[if gt IE 8]><!--> <html class=\"no-js\" lang=\"en\"> <!--<![endif]-->");

        String sHtmlText = "";
        //alipay
        if (cardType.equals("alipay")) {
            response.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head>");
            Map<String, String> sParaTemp = new HashMap<>();
            sParaTemp.put("service", AlipayConfig.service);
            sParaTemp.put("partner", AlipayConfig.partner);
            sParaTemp.put("seller_id", AlipayConfig.seller_id);
            sParaTemp.put("_input_charset", AlipayConfig.input_charset);
            sParaTemp.put("payment_type", AlipayConfig.payment_type);
            sParaTemp.put("notify_url", AlipayConfig.notify_url);
            sParaTemp.put("return_url", AlipayConfig.return_url);
            sParaTemp.put("anti_phishing_key", AlipayConfig.anti_phishing_key);
            sParaTemp.put("exter_invoke_ip", AlipayConfig.exter_invoke_ip);
            sParaTemp.put("out_trade_no", orderId);
            Order order = orderService.findOrderById(Long.parseLong(orderId));
            sParaTemp.put("subject", NullPaymentGatewayConstants.PAY_BODY);
            sParaTemp.put("total_fee", order.getTotal().toString());
//            sParaTemp.put("body", order.);
            sHtmlText = AlipaySubmit.buildRequest(sParaTemp, "get", "确认");
        }


        response.append("<body>");
        response.append(sHtmlText);
//        response.append("<form action=\"" +
//                paymentGatewayConfiguration.getTransparentRedirectReturnUrl() +
//                "\" method=\"POST\" id=\"NullPaymentGatewayRedirectForm\" name=\"NullPaymentGatewayRedirectForm\">");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.TRANSACTION_AMT
//                +"\" value=\"" + transactionAmount + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.ORDER_ID
//                +"\" value=\"" + orderId + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.GATEWAY_TRANSACTION_ID
//                +"\" value=\"" + gatewayT name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_LAST_FOUR
//                +"\" value=\"" + StringUtils.right(creditCardNumber, 4) + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_TYPE
//                +"\" value=\"" + cardType + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_EXP_DATE
//                +"\" value=\"" + creditCardExpDate + "\"/>");
//

//        response.append("<input type=\"hidden\" name=\"out_trade_no\" value=\"" + orderId + "\"/>");
//        response.append("<input type=\"hidden\" name=\"total_fee\" value=\"" + transactionAmount + "\"/>");
//
//        response.append("<input type=\"hidden\" name=\"trade_status\"  value=\"TRADE_SUCCESS\"/>");


//        response.append("<input type=\"submit\" value=\"Please Click Here To Complete Checkout\"/>");
//        response.append("</form>");
//        response.append("<script type=\"text/javascript\">");
//        response.append("document.getElementById('NullPaymentGatewayRedirectForm').submit();");
//        response.append("</script>");
        response.append("</body>");
        response.append("</html>");

        return response.toString();
    }

}
// @RequestMapping(value = "/null-checkout/process", method = RequestMethod.POST)
//    public @ResponseBody String processTransparentRedirectForm(HttpServletRequest request){
//        Map<String,String[]> paramMap = request.getParameterMap();
//
//        String transactionAmount = "";
//        String orderId="";
//        String billingFirstName = "";
//        String billingLastName = "";
//        String billingAddressLine1 = "";
//        String billingAddressLine2 = "";
//        String billingCity = "";
//        String billingState = "";
//        String billingZip = "";
//        String billingCountry = "";
//        String shippingFirstName = "";
//        String shippingLastName = "";
//        String shippingAddressLine1 = "";
//        String shippingAddressLine2 = "";
//        String shippingCity = "";
//        String shippingState = "";
//        String shippingZip = "";
//        String shippingCountry = "";
//        String creditCardName = "";
//        String creditCardNumber = "";
//        String creditCardExpDate = "";
//        String creditCardCVV = "";
//        String cardType = "UNKNOWN";
//
//        String resultMessage = "";
//        String resultSuccess = "";
//        String gatewayTransactionId = UUID.randomUUID().toString();
//
//        if (paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT) != null
//                && paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT).length > 0) {
//            transactionAmount = paramMap.get(NullPaymentGatewayConstants.TRANSACTION_AMT)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.ORDER_ID) != null
//                && paramMap.get(NullPaymentGatewayConstants.ORDER_ID).length > 0) {
//            orderId = paramMap.get(NullPaymentGatewayConstants.ORDER_ID)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_FIRST_NAME) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_FIRST_NAME).length > 0) {
//            billingFirstName = paramMap.get(NullPaymentGatewayConstants.BILLING_FIRST_NAME)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_LAST_NAME) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_LAST_NAME).length > 0) {
//            billingLastName = paramMap.get(NullPaymentGatewayConstants.BILLING_LAST_NAME)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE1) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE1).length > 0) {
//            billingAddressLine1 = paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE1)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE2) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE2).length > 0) {
//            billingAddressLine2 = paramMap.get(NullPaymentGatewayConstants.BILLING_ADDRESS_LINE2)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_CITY) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_CITY).length > 0) {
//            billingCity = paramMap.get(NullPaymentGatewayConstants.BILLING_CITY)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_STATE) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_STATE).length > 0) {
//            billingState = paramMap.get(NullPaymentGatewayConstants.BILLING_STATE)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_ZIP) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_ZIP).length > 0) {
//            billingZip = paramMap.get(NullPaymentGatewayConstants.BILLING_ZIP)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.BILLING_COUNTRY) != null
//                && paramMap.get(NullPaymentGatewayConstants.BILLING_COUNTRY).length > 0) {
//            billingCountry = paramMap.get(NullPaymentGatewayConstants.BILLING_COUNTRY)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_FIRST_NAME) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_FIRST_NAME).length > 0) {
//            shippingFirstName = paramMap.get(NullPaymentGatewayConstants.SHIPPING_FIRST_NAME)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_LAST_NAME) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_LAST_NAME).length > 0) {
//            shippingLastName = paramMap.get(NullPaymentGatewayConstants.SHIPPING_LAST_NAME)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE1) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE1).length > 0) {
//            shippingAddressLine1 = paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE1)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE2) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE2).length > 0) {
//            shippingAddressLine2 = paramMap.get(NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE2)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_CITY) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_CITY).length > 0) {
//            shippingCity = paramMap.get(NullPaymentGatewayConstants.SHIPPING_CITY)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_STATE) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_STATE).length > 0) {
//            shippingState = paramMap.get(NullPaymentGatewayConstants.SHIPPING_STATE)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_ZIP) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_ZIP).length > 0) {
//            shippingZip = paramMap.get(NullPaymentGatewayConstants.SHIPPING_ZIP)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.SHIPPING_COUNTRY) != null
//                && paramMap.get(NullPaymentGatewayConstants.SHIPPING_COUNTRY).length > 0) {
//            shippingCountry = paramMap.get(NullPaymentGatewayConstants.SHIPPING_COUNTRY)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NAME) != null
//                && paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NAME).length > 0) {
//            creditCardName = paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NAME)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NUMBER) != null
//                && paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NUMBER).length > 0) {
//            creditCardNumber = paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_NUMBER)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_EXP_DATE) != null
//                && paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_EXP_DATE).length > 0) {
//            creditCardExpDate = paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_EXP_DATE)[0];
//        }
//
//        if (paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_CVV) != null
//                && paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_CVV).length > 0) {
//            creditCardCVV = paramMap.get(NullPaymentGatewayConstants.CREDIT_CARD_CVV)[0];
//        }
//
//        CreditCardValidator visaValidator = new CreditCardValidator(CreditCardValidator.VISA);
//        CreditCardValidator amexValidator = new CreditCardValidator(CreditCardValidator.AMEX);
//        CreditCardValidator mcValidator = new CreditCardValidator(CreditCardValidator.MASTERCARD);
//        CreditCardValidator discoverValidator = new CreditCardValidator(CreditCardValidator.DISCOVER);
//
//        if (StringUtils.isNotBlank(transactionAmount) &&
//                StringUtils.isNotBlank(creditCardNumber) &&
//                StringUtils.isNotBlank(creditCardExpDate)) {
//
//            boolean validCard = false;
//            if (visaValidator.isValid(creditCardNumber)){
//                validCard = true;
//                cardType = "VISA";
//            } else if (amexValidator.isValid(creditCardNumber)) {
//                validCard = true;
//                cardType = "AMEX";
//            } else if (mcValidator.isValid(creditCardNumber)) {
//                validCard = true;
//                cardType = "MASTERCARD";
//            } else if (discoverValidator.isValid(creditCardNumber)) {
//                validCard = true;
//                cardType = "DISCOVER";
//            }
//
//            boolean validDateFormat = false;
//            boolean validDate = false;
//            String[] parsedDate = creditCardExpDate.split("/");
//            if (parsedDate.length == 2) {
//                String expMonth = parsedDate[0];
//                String expYear = parsedDate[1];
//                try {
//                    DateTime expirationDate = new DateTime(Integer.parseInt("20"+expYear), Integer.parseInt(expMonth), 1, 0, 0);
//                    expirationDate = expirationDate.dayOfMonth().withMaximumValue();
//                    validDate = expirationDate.isAfterNow();
//                    validDateFormat = true;
//                } catch (Exception e) {
//                    //invalid date format
//                }
//            }
//
//            if (!validDate || !validDateFormat) {
//                transactionAmount = "0";
//                resultMessage = "cart.payment.expiration.invalid";
//                resultSuccess = "false";
//            } else if (!validCard) {
//                transactionAmount = "0";
//                resultMessage = "cart.payment.card.invalid";
//                resultSuccess = "false";
//            } else {
//                resultMessage = "Success!";
//                resultSuccess = "true";
//            }
//
//        } else {
//            transactionAmount = "0";
//            resultMessage = "cart.payment.invalid";
//            resultSuccess = "false";
//        }
//
//        StringBuffer response = new StringBuffer();
//        response.append("<!DOCTYPE HTML>");
//        response.append("<!--[if lt IE 7]> <html class=\"no-js lt-ie9 lt-ie8 lt-ie7\" lang=\"en\"> <![endif]-->");
//        response.append("<!--[if IE 7]> <html class=\"no-js lt-ie9 lt-ie8\" lang=\"en\"> <![endif]-->");
//        response.append("<!--[if IE 8]> <html class=\"no-js lt-ie9\" lang=\"en\"> <![endif]-->");
//        response.append("<!--[if gt IE 8]><!--> <html class=\"no-js\" lang=\"en\"> <!--<![endif]-->");
//        response.append("<body>");
//        response.append("<form action=\"" +
//                paymentGatewayConfiguration.getTransparentRedirectReturnUrl() +
//                "\" method=\"POST\" id=\"NullPaymentGatewayRedirectForm\" name=\"NullPaymentGatewayRedirectForm\">");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.TRANSACTION_AMT
//                +"\" value=\"" + transactionAmount + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.ORDER_ID
//                +"\" value=\"" + orderId + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.GATEWAY_TRANSACTION_ID
//                +"\" value=\"" + gatewayTransactionId + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.RESULT_MESSAGE
//                +"\" value=\"" + resultMessage + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.RESULT_SUCCESS
//                +"\" value=\"" + resultSuccess + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_FIRST_NAME
//                +"\" value=\"" + billingFirstName + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_LAST_NAME
//                +"\" value=\"" + billingLastName + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_ADDRESS_LINE1
//                +"\" value=\"" + billingAddressLine1 + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_ADDRESS_LINE2
//                +"\" value=\"" + billingAddressLine2 + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_CITY
//                +"\" value=\"" + billingCity + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_STATE
//                +"\" value=\"" + billingState + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_ZIP
//                +"\" value=\"" + billingZip + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.BILLING_COUNTRY
//                +"\" value=\"" + billingCountry + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_FIRST_NAME
//                +"\" value=\"" + shippingFirstName + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_LAST_NAME
//                +"\" value=\"" + shippingLastName + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE1
//                +"\" value=\"" + shippingAddressLine1 + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_ADDRESS_LINE2
//                +"\" value=\"" + shippingAddressLine2 + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_CITY
//                +"\" value=\"" + shippingCity + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_STATE
//                +"\" value=\"" + shippingState + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_ZIP
//                +"\" value=\"" + shippingZip + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.SHIPPING_COUNTRY
//                +"\" value=\"" + shippingCountry + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_NAME
//                +"\" value=\"" + creditCardName + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_LAST_FOUR
//                +"\" value=\"" + StringUtils.right(creditCardNumber, 4) + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_TYPE
//                +"\" value=\"" + cardType + "\"/>");
//        response.append("<input type=\"hidden\" name=\"" + NullPaymentGatewayConstants.CREDIT_CARD_EXP_DATE
//                +"\" value=\"" + creditCardExpDate + "\"/>");
//
//        response.append("<input type=\"submit\" value=\"Please Click Here To Complete Checkout\"/>");
//        response.append("</form>");
//        response.append("<script type=\"text/javascript\">");
//        response.append("document.getElementById('NullPaymentGatewayRedirectForm').submit();");
//        response.append("</script>");
//        response.append("</body>");
//        response.append("</html>");
//
//        return response.toString();
//    }
//
//}
