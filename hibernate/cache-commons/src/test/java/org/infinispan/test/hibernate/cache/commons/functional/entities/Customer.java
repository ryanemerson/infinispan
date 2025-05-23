package org.infinispan.test.hibernate.cache.commons.functional.entities;
import java.io.Serializable;
import java.util.Set;

/**
 * Company customer
 *
 * @author Emmanuel Bernard
 * @author Kabir Khan
 */
public class Customer implements Serializable {
	Integer id;
	String name;
	// mapping added programmatically
	long version;

	private transient Set<Contact> contacts;

	public Customer() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String string) {
		name = string;
	}

	public Set<Contact> getContacts() {
		return contacts;
	}

	public void setContacts(Set<Contact> contacts) {
		this.contacts = contacts;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
}
