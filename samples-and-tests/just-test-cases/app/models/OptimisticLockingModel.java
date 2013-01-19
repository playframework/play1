package models;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;

import controllers.CRUD.Hidden;

import play.data.validation.Check;
import play.data.validation.CheckWith;
import play.db.jpa.GenericModel;
import play.mvc.Http.Request;

/**
 * Model which supports optimistic locking.
 *
 */
@MappedSuperclass
public class OptimisticLockingModel extends GenericModel {

    @Transient
    private transient Long initialVersion = null;

    @Id
    @GeneratedValue
    public Long id;

    
    @CheckWith(value=OptimisticLockingCheck.class, message="optimisticLocking.modelHasChanged")
    @Version
    @Column(nullable=false)
    /**
     * The version which will be automatically updated which each update. 
     */
    public Long version;
    
    public void setVersion(Long newVersion) {
        if (initialVersion == null)  {
            //If the model loaded via hibernate the setVersion-Method isn't called! 
            if (version != null) {
                initialVersion = version;
            } else {
                initialVersion = newVersion;
            }
        }
        version = newVersion;
    }

    public Long getId() {
        return id;
    }

     
    /**
     * Check with proof if the version of the current edited object is lesser
     * than the version in db.
     * Messagecode: optimisticLocking.modelHasChanged
     * Parameter: 2 the version of the edited model.
     * Parameter: 3 the version in the database.
     * Example-Message: The object was changed. Reload and do your changes again.
     *
     */
    public static class OptimisticLockingCheck extends Check {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isSatisfied(Object model, Object value) {            
            OptimisticLockingModel optimisticLockingModel = (OptimisticLockingModel) model;
            if ((optimisticLockingModel.initialVersion != null && 
                    optimisticLockingModel.version != null) && 
                    (optimisticLockingModel.initialVersion.longValue() > 
                     optimisticLockingModel.version.longValue())) {
                Long version = optimisticLockingModel.version;
                Long initialVersion = optimisticLockingModel.initialVersion ;
                setMessage(checkWithCheck.getMessage(), version != null ? version.toString() : "", 
                        initialVersion != null ? initialVersion.toString() : "");
                return false;
            } 
            return true;
        }
    }


}
