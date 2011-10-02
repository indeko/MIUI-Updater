package org.miuigermany.utils;

public class Update implements Comparable {

	private String nombre;
	private String url;
	private String descUrl;
	private int[] version;
	private String versionForApply;
	
	public Update(String nombre, String url, String descUrl, int[] version){
		this.nombre = nombre;
		this.url = url;
		this.descUrl = descUrl;
		this.version = version;
	}
	
	@Override
	public int compareTo(Object another) {
		return compararVersiones(0, (Update) another);
	}
	
	private int compararVersiones(int i, Update another){
		/*Devuelve 1 si la versión actual es MAYOR que a la que se compara,
		 * -1 si es MENOR y 0 si es IGUAL. 
		 */
		if(version[i] > another.getVersion()[i])
			return 1;
		else if(version[i] < another.getVersion()[i])
			return -1;
		else
			if(i == version.length-1)
				return 0;
			return compararVersiones(i+1, another);
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

	public int[] getVersion() {
		return version;
	}

	public void setVersion(int[] version) {
		this.version = version;
	}

	public String getVersionForApply() {
		return versionForApply;
	}

	public void setVersionForApply(String versionForApply) {
		this.versionForApply = versionForApply;
	}
}
