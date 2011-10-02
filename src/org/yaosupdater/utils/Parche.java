package org.yaosupdater.utils;


public class Parche {

	private String nombre;
	private String url;
	private String descUrl;
	private int[] versionMax;
	private int[] versionMin;
	
	public Parche(String nombre, String url, String descUrl, int[] versionMax, int[] versionMin){
		this.nombre = nombre;
		this.url = url;
		this.descUrl = descUrl;
		this.versionMax = versionMax;
		this.versionMin = versionMin;
	}
	
	public boolean aplicable(Update update){
		//El parche es aplicable siempre que la versión actual se encuentre entre el máximo y el mínimo incluídos ambos
		if((minEsMenor(0, update))&&(maxEsMayor(0, update))){
			return true;
		}else{
			return false;
		}
	}
	
	private boolean minEsMenor(int i, Update update){
		if(versionMin[i] < update.getVersion()[i]){
			return true;
		}else if(versionMin[i] == update.getVersion()[i]){
			if(i == versionMin.length-1){
				return true;
			}else{
				return minEsMenor(i+1, update);
			}
		}
		return false;
	}
	
	private boolean maxEsMayor(int i, Update update){
		if(versionMax[i] > update.getVersion()[i]){
			return true;
		}else if(versionMax[i] == update.getVersion()[i]){
			if(i == versionMax.length-1){
				return true;
			}else{
				return maxEsMayor(i+1, update);
			}
		}
		return false;
	}
	
	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getArchivo() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescUrl() {
		return descUrl;
	}

	public void setDescUrl(String descUrl) {
		this.descUrl = descUrl;
	}

	public int[] getVersionMax() {
		return versionMax;
	}

	public void setVersionMax(int[] versionMax) {
		this.versionMax = versionMax;
	}

	public int[] getVersionMin() {
		return versionMin;
	}

	public void setVersionMin(int[] versionMin) {
		this.versionMin = versionMin;
	}
}
