/*
  * Copyright 2008-2012 the original author or authors.
 *
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
 */

package com.immotor.controller.checkout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.exception.ServiceException;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.time.SystemTime;
import org.broadleafcommerce.common.vendor.service.exception.PaymentException;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.broadleafcommerce.core.web.checkout.model.*;
import org.broadleafcommerce.core.web.controller.checkout.BroadleafCheckoutController;
import org.broadleafcommerce.core.web.order.CartState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;

@Controller
public class CheckoutController extends BroadleafCheckoutController {
    private static final Log LOG = LogFactory.getLog(CheckoutController.class);
    @RequestMapping("/checkout")
    public String checkout(HttpServletRequest request, HttpServletResponse response, Model model,
                           @ModelAttribute("orderInfoForm") OrderInfoForm orderInfoForm,
                           @ModelAttribute("shippingInfoForm") ShippingInfoForm shippingForm,
                           @ModelAttribute("billingInfoForm") BillingInfoForm billingForm,
                           @ModelAttribute("giftCardInfoForm") GiftCardInfoForm giftCardInfoForm,
                           @ModelAttribute("customerCreditInfoForm") CustomerCreditInfoForm customerCreditInfoForm,
                           RedirectAttributes redirectAttributes) {
        return super.checkout(request, response, model, redirectAttributes);
    }

    @RequestMapping(value = "/checkout/savedetails", method = RequestMethod.POST)
    public String saveGlobalOrderDetails(HttpServletRequest request, Model model,
                                         @ModelAttribute("shippingInfoForm") ShippingInfoForm shippingForm,
                                         @ModelAttribute("billingInfoForm") BillingInfoForm billingForm,
                                         @ModelAttribute("giftCardInfoForm") GiftCardInfoForm giftCardInfoForm,
                                         @ModelAttribute("orderInfoForm") OrderInfoForm orderInfoForm, BindingResult result) throws ServiceException {
        return super.saveGlobalOrderDetails(request, model, orderInfoForm, result);
    }

    @RequestMapping(value = "/checkout/complete", method = RequestMethod.POST)
    public String processCompleteCheckoutOrderFinalized(RedirectAttributes redirectAttributes) throws PaymentException {
        return super.processCompleteCheckoutOrderFinalized(redirectAttributes);
    }

    @RequestMapping(value = "/checkout/cod/complete", method = RequestMethod.POST)
    public String processPassthroughCheckout(RedirectAttributes redirectAttributes)
            throws PaymentException, PricingException {
        return super.processPassthroughCheckout(redirectAttributes, PaymentType.COD);
    }

    @RequestMapping(value = "/checkout/save", method = RequestMethod.POST)
    public String saveOrder(HttpServletRequest request, Model model,
                            @ModelAttribute("shippingInfoForm") ShippingInfoForm shippingForm,
                            @ModelAttribute("billingInfoForm") BillingInfoForm billingForm,
                            @ModelAttribute("giftCardInfoForm") GiftCardInfoForm giftCardInfoForm,
                            @ModelAttribute("orderInfoForm") OrderInfoForm orderInfoForm, BindingResult result)
            throws PaymentException, PricingException {
        Order cart = CartState.getCart();
        try {
            cart.setEmailAddress(orderInfoForm.getEmailAddress());
            cart.setOrderNumber(new SimpleDateFormat("yyyyMMddHHmmssS").format(SystemTime.asDate()) + cart.getId());
            this.orderService.save(cart, Boolean.valueOf(true));
        } catch (PricingException var8) {
            LOG.error("Error when saving the email address for order confirmation to the cart", var8);
        }

        return this.getBaseConfirmationView();
    }

    @InitBinder
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        super.initBinder(request, binder);
    }

}
