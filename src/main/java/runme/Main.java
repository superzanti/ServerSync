package runme;

import java.io.IOException;

public class Main {
	private static Delete deleteOldMods = new Delete();

	public static void main(String[] args) throws InterruptedException, IOException  {
		System.out.println("Running main code");
		Thread t = new Thread(deleteOldMods);
		t.start();
	}

}
