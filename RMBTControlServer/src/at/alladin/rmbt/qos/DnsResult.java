/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.alladin.rmbt.qos;

import java.util.HashSet;

import at.alladin.rmbt.shared.hstoreparser.annotation.HstoreCollection;
import at.alladin.rmbt.shared.hstoreparser.annotation.HstoreKey;

/**
 * 
 * @author lb
 *
 */
public class DnsResult extends AbstractResult<DnsResult> {
	
	/**
	 * 
	 * @author lb
	 *
	 */
	public static class DnsEntry {
		@HstoreKey("dns_result_address")
		private String address;
		
		@HstoreKey("dns_result_ttl")
		private String ttl;
		
		@HstoreKey("dns_result_priority")
		private String priority;
		
		public DnsEntry() {
			
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getTtl() {
			return ttl;
		}

		public void setTtl(String ttl) {
			this.ttl = ttl;
		}

		public String getPriority() {
			return priority;
		}

		public void setPriority(String priority) {
			this.priority = priority;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "[address=" + address + ", ttl=" + ttl
					+ (priority != null ? ", priority=" + priority : "" ) + "]";
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((address == null) ? 0 : address.hashCode());
			result = prime * result
					+ ((priority == null) ? 0 : priority.hashCode());
			result = prime * result + ((ttl == null) ? 0 : ttl.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DnsEntry other = (DnsEntry) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			if (priority == null) {
				if (other.priority != null)
					return false;
			} else if (!priority.equals(other.priority))
				return false;
			if (ttl == null) {
				if (other.ttl != null)
					return false;
			} else if (!ttl.equals(other.ttl))
				return false;
			return true;
		}
	}
	
	@HstoreKey("dns_objective_host")
	private String host;

	@HstoreKey("dns_result_info")
	private String info;
	
	@HstoreKey("dns_result_status")
	private String status;

	@HstoreKey("dns_objective_resolver")
	private String resolver;
	
	@HstoreKey("dns_objective_dns_record")
	private String record;
	
	@HstoreKey("dns_objective_timeout")
	private String timeout;
	
	@HstoreKey("dns_result_entries_found")
	private String entriesFound;
	
	@HstoreKey("dns_result_duration")
	private String duration;
	
	@HstoreKey("dns_result_entries")
	@HstoreCollection(DnsEntry.class)
	private HashSet<DnsEntry> resultEntries;

	/**
	 * 
	 */
	public DnsResult() {
		
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getResolver() {
		return resolver;
	}

	public void setResolver(String resolver) {
		this.resolver = resolver;
	}

	public String getRecord() {
		return record;
	}

	public void setRecord(String record) {
		this.record = record;
	}

	public HashSet<DnsEntry> getResultEntries() {
		return resultEntries;
	}

	public void setResultEntries(HashSet<DnsEntry> resultEntries) {
		this.resultEntries = resultEntries;
	}

	public String getEntriesFound() {
		return entriesFound;
	}

	public void setEntriesFound(String entriesFound) {
		this.entriesFound = entriesFound;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTimeout() {
		return timeout;
	}

	public void setTimeout(String timeout) {
		this.timeout = timeout;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	@Override
	public String toString() {
		return "DnsResult [host=" + host + ", info=" + info + ", status="
				+ status + ", resolver=" + resolver + ", record=" + record
				+ ", timeout=" + timeout + ", entriesFound=" + entriesFound
				+ ", duration=" + duration + ", resultEntries=" + resultEntries
				+ ", operator=" + operator + ", onFailure=" + onFailure
				+ ", onSuccess=" + onSuccess + ", evaluate=" + evaluate
				+ ", endTimeNs=" + endTimeNs + ", startTimeNs=" + startTimeNs
				+ ", testDuration=" + testDuration + "]";
	}
}