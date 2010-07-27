package play.db;

import java.util.List;

public interface ModelLoader {

    public Model findById(Object id);
    public List<Model> fetch(int offset, int size, String orderBy, String orderDirection);
    public Long count();
    public List<Model> search(List<String> properties, String keywords, int offset, int size, String orderBy, String orderDirection);
    public Long countSearch(List<String> properties, String keywords);
    public void deleteAll();
    public Class<?> _getKeyType();
    public List<ModelProperty> listProperties();
    
}
