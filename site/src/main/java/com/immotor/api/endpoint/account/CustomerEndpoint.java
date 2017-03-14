package com.immotor.api.endpoint.account;

import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.web.api.wrapper.CustomerWrapper;
import org.broadleafcommerce.profile.core.domain.*;
import org.broadleafcommerce.profile.core.service.CustomerPhoneService;
import org.broadleafcommerce.profile.core.service.PhoneService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a reference REST API endpoint for account. This can be modified, used as is, or removed.
 * The purpose is to provide an out of the box RESTful user service implementation, but also
 * to allow the implementor to have fine control over the actual API, URIs, and general JAX-RS annotations.
 *
 * @author billyang on 2017/3/10.
 */
@RestController
@RequestMapping(value = "/account",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class CustomerEndpoint extends org.broadleafcommerce.core.web.api.endpoint.customer.CustomerEndpoint {

    @Resource(name = "blNullPaymentGatewayController")
    protected PaymentGatewayAbstractController paymentGatewayAbstractController;

    @Resource(name = "blCustomerPhoneService")
    protected CustomerPhoneService customerPhoneService;

    @Resource(name = "blPhoneService")
    protected PhoneService phoneService;

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public CustomerWrapper login(HttpServletRequest request,
                                 @RequestParam("id") Long id,
                                 @RequestParam(value = "phone") String phone,
                                 @RequestParam(value = "email", defaultValue = "") String email,
                                 @RequestParam(value = "avatar") String avatar,
                                 @RequestParam(value = "area_code", defaultValue = "") String area_code,
                                 @RequestParam("nickname") String nickname) {
        Customer c = customerService.readCustomerById(id);

        if (null == c) {
            c = customerService.createCustomerFromId(id);
            c.setUsername(phone);
            c.setFirstName(nickname);
            if (!"".equals(email)) {
                c.setEmailAddress(email);
            }
            //add avatar
            CustomerAttribute ca = new CustomerAttributeImpl();
            ca.setName("url");
            ca.setValue(avatar);
            ca.setCustomer(c);
            Map map = new HashMap<String, CustomerAttribute>();
            map.put("avatar", ca);
            c.setCustomerAttributes(map);
            // add phone
//            if (!"".equals(phone)) {
            List<CustomerPhone> list = new ArrayList<>();
            Phone p = phoneService.create();
            p.setPhoneNumber(phone);
            p.setCountryCode(area_code);
            CustomerPhone cp = customerPhoneService.create();
            cp.setPhone(p);
            cp.setCustomer(c);
            list.add(cp);
            c.setCustomerPhones(list);
            customerService.saveCustomer(c, true);
            customerService.createRegisteredCustomerRoles(c);
//            }
        }
        CustomerWrapper customerWrapper = (CustomerWrapper) this.context.getBean(CustomerWrapper.class.getName());
        customerWrapper.wrapSummary(c, request);
        return customerWrapper;
    }
}
