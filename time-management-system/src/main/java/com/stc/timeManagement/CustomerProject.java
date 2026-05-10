package com.stc.timeManagement;

public class CustomerProject {
    private static int idCounter = 1000; // 1000'den başlasın
    private int id;
    private String name;

    // Sadece isim alan constructor (ID'yi otomatik atar)
    public CustomerProject(String name) {
        this.id = idCounter++; 
        this.name = name;
    }

    // Mevcut ID ve isim alan constructor (Geriye dönük uyumluluk için)
    public CustomerProject(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    
    @Override
    public String toString() { return name; }

	public void setName(String newValue) {
		// TODO Auto-generated method stub
		this.name = newValue;
	}
}