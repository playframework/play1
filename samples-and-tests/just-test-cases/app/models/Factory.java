package models;

public class Factory  {
    
    public static enum Color { 
        RED, GREEN, BLUE; 
        public String toString() { 
            return "Color " + this.name(); 
        } 
    }
    
    public long number;
    
    public String name;

    public Color color;
    
    public String toString() {
        return name;
    }
    
}

