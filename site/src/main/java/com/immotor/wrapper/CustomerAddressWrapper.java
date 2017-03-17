package com.immotor.wrapper;

import org.broadleafcommerce.core.web.api.wrapper.APIWrapper;
import org.broadleafcommerce.core.web.api.wrapper.BaseWrapper;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.*;

/**
 * Created by billyang on 2017/3/17.
 */
@XmlRootElement(
        name = "customerAddress"
)
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomerAddressWrapper extends BaseWrapper implements APIWrapper<CustomerAddress> {

    @XmlElement
    protected Long id;

    @XmlElement
    protected String addressName;

    @XmlElement
    protected Long customerId;

    @Override
    public void wrapDetails(CustomerAddress customerAddress, HttpServletRequest httpServletRequest) {
        this.id = customerAddress.getId();
        this.addressName = customerAddress.getAddressName();
        this.customerId = customerAddress.getCustomer().getId();
        AddressWrapper wrapper = (AddressWrapper) this.context.getBean(AddressWrapper.class.getName());
        wrapper.wrapSummary(customerAddress.getAddress(), httpServletRequest);
        this.address = wrapper;
    }

    @Override
    public void wrapSummary(CustomerAddress customerAddress, HttpServletRequest httpServletRequest) {
        this.wrapDetails(customerAddress, httpServletRequest);
    }

    @XmlElementWrapper
    protected AddressWrapper address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public AddressWrapper getAddress() {
        return address;
    }

    public void setAddress(AddressWrapper address) {
        this.address = address;
    }


}
