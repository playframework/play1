package play.data.binding;

class Data6 {

    public String name;
    
    private Data6() {
        // this should be used by Play Binder
    }
    
    public Data6(String name) {
        // this should be used by application code
        this.name = name;
    }
}
