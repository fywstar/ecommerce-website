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

import com.alipay.util.httpClient.AlipayNotify;
import com.immotor.sample.vendor.nullPaymentGateway.service.payment.NullPaymentGatewayConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.payment.dto.PaymentResponseDTO;
import org.broadleafcommerce.common.payment.service.PaymentGatewayConfiguration;
import org.broadleafcommerce.common.payment.service.PaymentGatewayWebResponseService;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This is a sample implementation of {@link PaymentGatewayAbstractController}
 * that mimics what a real Payment Gateway Controller might look like.
 * This will handle translating the "fake response" from our NullPaymentGateway
 * so that it can be processed in the system.
 * <p>
 * In order to use this sample controller, you will need to component scan
 * the package "com.immotor.sample".
 * <p>
 * This should NOT be used in production, and is meant solely for demonstration
 * purposes only.
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Controller("blNullPaymentGatewayController")
@RequestMapping("/" + NullPaymentGatewayController.GATEWAY_CONTEXT_KEY)
public class NullPaymentGatewayController extends PaymentGatewayAbstractController {

    protected static final Log LOG = LogFactory.getLog(NullPaymentGatewayController.class);
    protected static final String GATEWAY_CONTEXT_KEY = "null-checkout";

    @Resource(name = "blNullPaymentGatewayWebResponseService")
    protected PaymentGatewayWebResponseService paymentGatewayWebResponseService;

    @Resource(name = "blNullPaymentGatewayConfiguration")
    protected PaymentGatewayConfiguration paymentGatewayConfiguration;
    @Resource(name = "blOrderService")
    protected OrderService orderService;

    @Override
    public void handleProcessingException(Exception e, RedirectAttributes redirectAttributes) throws PaymentException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("A Processing Exception Occurred for " + GATEWAY_CONTEXT_KEY +
                    ". Adding Error to Redirect Attributes.");
        }

        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR, getProcessingErrorMessage());
    }

    @Override
    public void handleUnsuccessfulTransaction(Model model, RedirectAttributes redirectAttributes,
                                              PaymentResponseDTO responseDTO) throws PaymentException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("The Transaction was unsuccessful for " + GATEWAY_CONTEXT_KEY +
                    ". Adding Errors to Redirect Attributes.");
        }
        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR,
                responseDTO.getResponseMap().get(NullPaymentGatewayConstants.RESULT_MESSAGE));
    }

    @Override
    public String getGatewayContextKey() {
        return GATEWAY_CONTEXT_KEY;
    }

    @Override
    public PaymentGatewayWebResponseService getWebResponseService() {
        return paymentGatewayWebResponseService;
    }

    @Override
    public PaymentGatewayConfiguration getConfiguration() {
        return paymentGatewayConfiguration;
    }

    @Override
    @RequestMapping(value = "/return", method = RequestMethod.GET)
    public String returnEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
                                 Map<String, String> pathVars) throws PaymentException {
        String result = validateAliPay(model, request, redirectAttributes);
        if ("C".equals(result)) {
            return super.process(model, request, redirectAttributes);
        } else if ("Y".equals(result)) {
            return this.getOrderReviewRedirect();
        }
        return this.getErrorViewRedirect();
    }

    @RequestMapping(value = "/notify", method = RequestMethod.POST)
    public String notifyEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
                                 Map<String, String> pathVars) throws PaymentException {
        String result = validateAliPay(model, request, redirectAttributes);
        if ("C".equals(result)) {
            String view = super.process(model, request, redirectAttributes);
            if (this.getOrderReviewRedirect().equals(view)) {
                return "success";
            }

        } else if ("Y".equals(result)) {
            return "success";
        }
        return "failed";
    }

    @Override
    @RequestMapping(value = "/error", method = RequestMethod.GET)
    public String errorEndpoint(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes,
                                Map<String, String> pathVars) throws PaymentException {
        redirectAttributes.addAttribute(PAYMENT_PROCESSING_ERROR,
                request.getParameter(PAYMENT_PROCESSING_ERROR));
        return getOrderReviewRedirect();
    }

    /**
     * @param model
     * @param request
     * @param redirectAttributes
     * @return Y: already success;  C: continue; {other}:failed
     */
    protected String validateAliPay(Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Map<String, String> params = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }
        String trade_status = new String(request.getParameter("trade_status"));
        String orderId = request.getParameter("out_trade_no");

        if (AlipayNotify.verify(params)) {
            //
            if (trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")) {
                Order order = orderService.findOrderById(Long.parseLong(orderId));
                if (order != null || !order.getStatus().equals(OrderStatus.IN_PROCESS)) {
                    return "C";
                }
                return "Y";
            }
            return "unsuccessful payment";
        } else {
            return "verify failed";
        }

    }


}
