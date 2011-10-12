package models;

import play.Logger;

import java.io.Serializable;

public class CompositeIdPk implements Serializable {
	
	private Long compositeIdForeignA;

	private Long compositeIdForeignB;
	
	public CompositeIdPk() {
	}

	public CompositeIdPk(Long compositeIdForeignA, Long compositeIdForeignB) {
		this.compositeIdForeignA = compositeIdForeignA;
		this.compositeIdForeignB = compositeIdForeignB;
	}

	public Long getCompositeIdForeignA() {
		return compositeIdForeignA;
	}

	public void setCompositeIdForeignA(Long compositeIdForeignA) {
		this.compositeIdForeignA = compositeIdForeignA;
	}

	public Long getCompositeIdForeignB() {
		return compositeIdForeignB;
	}

	public void setCompositeIdForeignB(Long compositeIdForeignB) {
		this.compositeIdForeignB = compositeIdForeignB;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((compositeIdForeignA == null) ? 0 : compositeIdForeignA
						.hashCode());
		result = prime
				* result
				+ ((compositeIdForeignB == null) ? 0 : compositeIdForeignB
						.hashCode());
        Logger.info("hashCode " + result);

        return result;
	}

	@Override
	public boolean equals(Object obj) {
        Logger.info("CompositeIdPk " + compositeIdForeignA + " " + compositeIdForeignB);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompositeIdPk other = (CompositeIdPk) obj;
		if (compositeIdForeignA == null) {
			if (other.compositeIdForeignA != null)
				return false;
		} else if (!compositeIdForeignA.equals(other.compositeIdForeignA))
			return false;
		if (compositeIdForeignB == null) {
			if (other.compositeIdForeignB != null)
				return false;
		} else if (!compositeIdForeignB.equals(other.compositeIdForeignB))
			return false;
		return true;
	}
}
