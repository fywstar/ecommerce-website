package com.immotor.api.endpoint.account;

import com.immotor.wrapper.CustomerAddressWrapper;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.web.api.wrapper.CustomerWrapper;
import org.broadleafcommerce.profile.core.domain.*;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.broadleafcommerce.profile.core.service.CustomerAddressService;
import org.broadleafcommerce.profile.core.service.CustomerPhoneService;
import org.broadleafcommerce.profile.core.service.PhoneService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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

    @Resource(name = "blAddressService")
    protected AddressService addressService;

    @Resource(name = "blCustomerAddressService")
    protected CustomerAddressService customerAddressService;

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

    @ResponseBody
    @RequestMapping(value = "/address", method = RequestMethod.POST)
    public Object addCustomerAddress(HttpServletRequest request, @RequestParam(value = "firstName") String firstName,
                                     @RequestParam(value = "lastName") String lastName,
                                     @RequestParam(value = "customerId") Long customerId,
                                     @RequestParam(value = "postalCode") String postalCode,
                                     @RequestParam(value = "phoneNumber") String phoneNumber,
                                     @RequestParam(value = "country") String country,
                                     @RequestParam(value = "city") String city,
                                     @RequestParam(value = "state") String state,
                                     @RequestParam(value = "address1") String address1,
                                     @RequestParam(value = "address2", defaultValue = "", required = false) String address2,
                                     @RequestParam(value = "addressName") String addressName,
                                     @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {

        Long addressId = null;
        try {
            Customer customer = customerService.readCustomerById(customerId);
            if (null == customer)
                return false;
            Address address = addressService.create();
            address.setAddressLine1(address1);
            address.setAddressLine2(address2);
            address.setFirstName(firstName);
            address.setLastName(lastName);
            address.setCounty(country);
            address.setCity(city);
            address.setActive(true);
            address.setStateProvinceRegion(state);
            address.setPostalCode(postalCode);
            Phone phone = phoneService.create();
            phone.setPhoneNumber(phoneNumber);
            address.setPhonePrimary(phone);
            addressService.saveAddress(address);
            CustomerAddress customerAddress = customerAddressService.create();
            customerAddress.setCustomer(customerService.readCustomerById(customerId));
            customerAddress.setAddress(address);
            customerAddress.setAddressName(addressName);
            customerAddress = customerAddressService.saveCustomerAddress(customerAddress);
            addressId = customerAddress.getId();
            if (isDefault) {
                customerAddressService.makeCustomerAddressDefault(customerAddress.getId(), customerAddress.getCustomer().getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return addressId;
    }


    @ResponseBody
    @RequestMapping(value = "/address/{customerAddressId}", method = RequestMethod.DELETE)
    public Object removeCustomerAddress(HttpServletRequest request, @PathVariable(value = "customerAddressId") Long customerAddressId,
                                        @RequestParam(value = "customerId") Long customerId) {
        CustomerAddress e = customerAddressService.readCustomerAddressById(customerAddressId);
        Customer customer = customerService.readCustomerById(customerId);
        if (e != null && e.getCustomer().equals(customer)) {
            customerAddressService.deleteCustomerAddressById(customerAddressId);
            return true;
        }
        return false;
    }

    @ResponseBody
    @RequestMapping(value = "/address/{customerAddressId}", method = RequestMethod.GET)
    public Object viewCustomerAddress(HttpServletRequest request, @PathVariable(value = "customerAddressId") Long customerAddressId,
                                      @RequestParam(value = "customerId") Long customerId) {
        CustomerAddress customerAddress = this.customerAddressService.readCustomerAddressById(customerAddressId);
        Customer customer = customerService.readCustomerById(customerId);
        if (customerAddress != null && customerAddress.getCustomer().equals(customer)) {
            CustomerAddressWrapper wrapper = (CustomerAddressWrapper) this.context.getBean(CustomerAddressWrapper.class.getName());
            wrapper.wrapDetails(customerAddress, request);
            return wrapper;
        }
        return null;
    }

    @ResponseBody
    @RequestMapping(value = "/address/{customerAddressId}", method = RequestMethod.POST)
    public Object updateCustomerAddress(HttpServletRequest request, @PathVariable(value = "customerAddressId") Long customerAddressId,
                                        @RequestParam(value = "firstName") String firstName,
                                        @RequestParam(value = "lastName") String lastName,
                                        @RequestParam(value = "customerId") Long customerId,
                                        @RequestParam(value = "postalCode") String postalCode,
                                        @RequestParam(value = "phoneNumber") String phoneNumber,
                                        @RequestParam(value = "country") String country,
                                        @RequestParam(value = "city") String city,
                                        @RequestParam(value = "state") String state,
                                        @RequestParam(value = "address1") String address1,
                                        @RequestParam(value = "address2", defaultValue = "") String address2,
                                        @RequestParam(value = "addressName") String addressName,
                                        @RequestParam(value = "isDefault", defaultValue = "false") boolean isDefault) {
        try {
            CustomerAddress customerAddress = this.customerAddressService.readCustomerAddressById(customerAddressId);
            if (!customerAddress.getCustomer().getId().equals(customerId)) {
                return false;
            }
            Address address = customerAddress.getAddress();
            address.setAddressLine1(address1);
            address.setAddressLine2(address2);
            address.setFirstName(firstName);
            address.setLastName(lastName);
            address.setCounty(country);
            address.setCity(city);
            address.setActive(true);
            address.setStateProvinceRegion(state);
            address.setPostalCode(postalCode);

            if (!phoneNumber.equals(address.getPhonePrimary().getPhoneNumber())) {
                address.getPhonePrimary().setPhoneNumber(phoneNumber);
                phoneService.savePhone(address.getPhonePrimary());
            }
            addressService.saveAddress(address);

            if (addressName.equals(customerAddress.getAddressName())) {
                customerAddress.setAddressName(addressName);
                customerAddressService.saveCustomerAddress(customerAddress);
            }

            if (!isDefault == address.isDefault()) {
                customerAddressService.makeCustomerAddressDefault(customerAddress.getId(), customerAddress.getCustomer().getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @ResponseBody
    @RequestMapping(value = "/address", method = RequestMethod.GET)
    public Object getCustomerAddressList(HttpServletRequest request,
                                         @RequestParam(value = "customerId") Long customerId) {
        List<CustomerAddress> addressList = this.customerAddressService.readActiveCustomerAddressesByCustomerId(customerId);
        List<CustomerAddressWrapper> result = new ArrayList<>();
        if (addressList != null) {
            for (CustomerAddress address : addressList) {
                CustomerAddressWrapper wrapper = (CustomerAddressWrapper) this.context.getBean(CustomerAddressWrapper.class.getName());
                wrapper.wrapDetails(address, request);
                result.add(wrapper);
            }
        }
        return result;
    }
}
