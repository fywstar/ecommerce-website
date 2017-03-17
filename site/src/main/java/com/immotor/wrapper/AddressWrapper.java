package com.immotor.wrapper;

import org.broadleafcommerce.core.web.api.wrapper.APIWrapper;
import org.broadleafcommerce.core.web.api.wrapper.BaseWrapper;
import org.broadleafcommerce.profile.core.domain.Address;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by billyang on 2017/3/17.
 */
@XmlRootElement(
        name = "Address"
)
@XmlAccessorType(XmlAccessType.FIELD)
public class AddressWrapper extends BaseWrapper implements APIWrapper<Address> {

    @XmlElement
    protected Long id;

    @XmlElement
    protected String AddressLine1;

    @XmlElement
    protected String AddressLine2;

    @XmlElement

    protected String city;

    @XmlElement
    protected String stateProvinceRegion;

    @XmlElement
    protected String postalCode;

    @XmlElement
    protected boolean isDefault;

    @XmlElement
    protected String firstName;

    @XmlElement
    protected String lastName;

    @XmlElement
    protected String phoneNumber;

    @Override
    public void wrapDetails(Address address, HttpServletRequest httpServletRequest) {
        this.id = address.getId();
        this.AddressLine1 = address.getAddressLine1();
        this.AddressLine2 = address.getAddressLine2();
        this.city = address.getCity();
        this.stateProvinceRegion = address.getStateProvinceRegion();
        this.postalCode = address.getPostalCode();
        this.isDefault = address.isDefault();
        this.firstName = address.getFirstName();
        this.lastName = address.getLastName();
        this.phoneNumber = address.getPhonePrimary().getPhoneNumber();
    }

    @Override
    public void wrapSummary(Address address, HttpServletRequest httpServletRequest) {
        this.wrapDetails(address, httpServletRequest);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddressLine1() {
        return AddressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        AddressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return AddressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        AddressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvinceRegion() {
        return stateProvinceRegion;
    }

    public void setStateProvinceRegion(String stateProvinceRegion) {
        this.stateProvinceRegion = stateProvinceRegion;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}
