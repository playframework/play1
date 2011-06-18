package play.data.binding;

import play.data.binding.As;
import play.data.binding.NoBinding;

import java.lang.annotation.Annotation;

class BindingAnnotations {
    public final Annotation[] annotations;
    private String[] profiles = null;
    private String[] noBindingProfiles = null;

    public BindingAnnotations() {
        this.annotations = null;
    }

    public BindingAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public BindingAnnotations(Annotation[] annotations, String[] profiles) {
        this.annotations = annotations;
        this.profiles = profiles;
    }

    public String[] getProfiles() {
        if (profiles != null) {
            return profiles;
        }

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(As.class)) {
                    As as = ((As) annotation);
                    profiles = as.value();
                }
                if (annotation.annotationType().equals(NoBinding.class)) {
                    NoBinding bind = ((NoBinding) annotation);
                    profiles = bind.value();
                }
            }
        }
        if (profiles==null) {
            profiles = new String[0];
        }

        return profiles;
    }

    private String[] getNoBindingProfiles() {
        if (noBindingProfiles!=null) {
            return noBindingProfiles;
        }

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(NoBinding.class)) {
                    NoBinding bind = ((NoBinding) annotation);
                    noBindingProfiles = bind.value();
                }
            }
        }

        if (noBindingProfiles==null) {
            noBindingProfiles = new String[0];
        }
        return noBindingProfiles;
    }

    public boolean checkNoBinding() {

        final String[] _profiles = getProfiles();
        final String[] _noBindingProfiles = getNoBindingProfiles();

        if (_noBindingProfiles.length>0) {
            for (String l : _noBindingProfiles) {
                if ("*".equals(l)) {
                    return true;
                }
                if (_profiles.length>0) {
                    for (String p : _profiles) {
                        if (l.equals(p) || "*".equals(p)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


}
