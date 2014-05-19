import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

public class Steganographie {

	public static void arrayToImage(int[][] image, String filename) throws FileNotFoundException
	{	
		String magic = "P2";
		int width = image[0].length;
		int height = image.length;
		PrintStream ps = new PrintStream(new FileOutputStream(filename));
		
		ps.println(magic);
		ps.println(width + " " + height); 
		ps.println("255");

		for(int i = 0; i < image.length; i++)
		{
			for(int j = 0; j < image[0].length; j++)
				ps.print(image[i][j] + " ");
			
			ps.println();
		}
		
		ps.close();
	}
	
	public static int[][] imageToArray(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(filename));
		int[][] map = null;

		// On regarde le "nombre magique" du fichier PGM, indiquant le type de fichier
		String magic = br.readLine();
		if (!"P2".equals(magic)) 
		{ 
			System.err.println("Format incompatible");
			br.close();
			return map;
		}
		
		// On continue la lecture de la suite de l'entête qui contient la hauteur et la largeur de l'image
		String line = br.readLine();
		// Toute ligne commençant par # est sautée (il s'agit d'un commentaire)
		while (line.startsWith("#")) 
		{
			line = br.readLine();
		}
		
		Scanner sc = new Scanner(line);
		int width = sc.nextInt();
		int height = sc.nextInt();

		// On continue la lecture, la dernière partie de l'entête contient la valeur maximum des données, mais on ne s'en sert pas
		line = br.readLine();
		int count = 0;

		map = new int[height][width];	// following lines contain pixel values

		while ((line = br.readLine()) != null)
		{
			sc = new Scanner(line);
			while (sc.hasNext())
			{
				// On stocke la valeur du pixel sur lequel on est dans le tableau
				int pixvalue = sc.nextInt(); 
				map[count / width][count % width] = pixvalue;
				count++;
			}
		}
		
		br.close();
		sc.close();
		return map;
	}
	
	public static void decode(int[][] image, int nbBits)
	{
		int count = 0;
		String charcode = "";
		String hidden = "";
		String endSequence = "ÿ";

		System.out.println("Calcul en cours...");
		
		for(int i = 0; i < image.length; i++)
		{
			for(int j = 0; j < image[0].length; j++)
			{
				if(!hidden.endsWith(endSequence))
				{
					String charbits = "";
					for(int k = 0; k < nbBits; k++)
						charbits += Integer.toBinaryString((image[i][j] & 1 << k) >> k);
					
					charbits = new StringBuilder(charbits).reverse().toString();
					
					while(charbits.length() < nbBits || charbits.length() == 0)
						charbits = "0" + charbits;
					
					if(count % 8 == 0 && count > 0)
						hidden += (char)Integer.parseInt(charcode.substring(count - 8, count), 2);
				
					charcode += charbits;
					count++;
				}
			}
		}
		
		// On supprime la séquence de fin de message du message caché
		hidden = hidden.substring(0, hidden.length() - endSequence.length());
		System.out.println("\n" + hidden);
	}
	
	public static void encode(int[][] image, String message, int nbBits, String dest) throws FileNotFoundException
	{
		message += "ÿ";
		byte[] messageBytes = message.getBytes();
		String binaryMessage = "";
		
		// Conversion du message en binaire
		for(int i = 0; i < messageBytes.length; i++)
		{
			String charbincode = Integer.toBinaryString(messageBytes[i]);
			// Les 0 du début étant supprimés lors de la conversion, on les rajoute
			while(charbincode.length() < 8)
				charbincode = "0" + charbincode;
			
			binaryMessage += charbincode;
		}
		
		// On cache chaque bit du message sur le bit de poids faible de chaque pixel
		int count = 0;
		
		for(int i = 0; i < image.length; i++)
		{
			for(int j = 0; j < image[0].length; j++)
			{
				if(count < binaryMessage.length() - nbBits)
				{
					String pixvalue = Integer.toBinaryString(image[i][j]);
					// Les 0 du début étant supprimés lors de la conversion, on les rajoute
					while(pixvalue.length() < 8)
						pixvalue = "0" + pixvalue;
				
					StringBuilder pixbuilder = new StringBuilder(pixvalue);
					
					// On remplace nbBits bits du pixel par nbBits du message binaire
					for(int k = 0; k < nbBits; k++)
					{
						pixbuilder.setCharAt(8 - nbBits + k, binaryMessage.charAt(count + k));
					}
					
					image[i][j] = Integer.parseInt(pixbuilder.toString(), 2);
					count += nbBits; 
				}
			}
		}
		
		arrayToImage(image, dest);
		System.out.println("\nLe message a été caché dans l'image '" + dest + "' avec succès.");
	}
	
	public static void main(String[] args) throws IOException {

		System.out.println("/////////////////////////////////////////////////////////");
		System.out.println("//                 STEGANOGRAPHIE                      //");
		System.out.println("//      (Décodage et Encodage de messages cachés       //");
		System.out.println("//                dans des images)                     //");
		System.out.println("/////////////////////////////////////////////////////////");
		System.out.print("\n");
		
		Scanner sc = new Scanner(System.in);
		System.out.println("Entrez le nom de l'image (au format PGM Type-2) que vous souhaitez traiter");
		String filename = sc.nextLine();
		// On crée un tableau à partir de l'image
		int[][] image = imageToArray(filename);
		System.out.println("Que voulez vous faire ?\n0 : Chercher un message caché\n1 : Cacher un message");
		int choice = sc.nextInt();
		sc.nextLine();
		
		switch(choice)
		{
			case 0:
				System.out.println("Sur combien de bits par pixel chercher le message ?");
				int nbBits = sc.nextInt();
				sc.nextLine();
				decode(image, nbBits);
				break;
			case 1:
				System.out.println("Ecrivez votre message :");
				String hidden = sc.nextLine();
				System.out.println("Sur combien de bits voulez-vous le coder ? (1 - 8)");
				nbBits = sc.nextInt();
				sc.nextLine();
				System.out.println("Entrez le nom de l'image à générer :");
				String dest = sc.nextLine();
				encode(image, hidden, nbBits, dest);
				break;
			default:
				System.out.println("Vous n'avez pas entré de valeur valide. Fermeture...");
				System.exit(0);				
		}
		
		sc.close();
	}
}