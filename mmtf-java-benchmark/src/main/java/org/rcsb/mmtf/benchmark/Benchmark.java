package org.rcsb.mmtf.benchmark;

import io.Directories;
import io.HadoopSequenceFileConverter;
import io.LineFile;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.rcsb.mmtf.utils.Lines;
import util.Timer;

/**
 *
 * Main class to run all the benchmarks.
 *
 * @author Antonin Pavelka
 */
public class Benchmark {

	private Directories dirs;

	private String[] selectedCodes = {"3j3q"};

	public Benchmark(String path) {
		dirs = new Directories(new File(path));
	}

	public void downloadHadoopSequenceFiles() {
		DatasetGenerator d = new DatasetGenerator(dirs);
		System.out.println("Downloading Hadoop sequence files:");
		Timer.start("hsf-download");
		d.downloadHadoopSequenceFiles();
		Timer.stop("hsf-download");
		Timer.print();
	}

	/**
	 * Downloads the whole PDB in MMTF, PDB and mmCIF file format.
	 */
	public void downloadWholeDatabase() {
		DatasetGenerator d = new DatasetGenerator(dirs);

		downloadHadoopSequenceFiles();

		System.out.println("Downloading MMTF files:");
		Timer.start("mmtf-download");
		d.downloadMmtf();
		Timer.stop("mmtf-download");
		Timer.print();

		System.out.println("Downloading PDB files:");
		Timer.start("pdb-download");
		d.downloadPdb();
		Timer.stop("pdb-download");
		Timer.print();

		System.out.println("Downloading mmCIF files:");
		Timer.start("mmcif-download");
		d.downloadCif();
		Timer.stop("mmcif-download");
		Timer.print();

		System.out.println("Downloading Hadoop sequence file:");
		Timer.start("hsf-download");
		d.downloadHadoopSequenceFiles();
		Timer.stop("hsf-download");
		Timer.print();

	}

	private void transformHsf() throws IOException {
		HadoopSequenceFileConverter.convert(dirs.getHsfReducedOriginal().toString(),
			dirs.getHsfReducedOriginalUntared().toString(),
			dirs.getHsfReduced().toString());
		HadoopSequenceFileConverter.convert(dirs.getHsfFullOriginal().toString(),
			dirs.getHsfFullOriginalUntared().toString(),
			dirs.getHsfFull().toString());
	}

	public void benchmarkHadoopSequenceFiles() throws IOException {
		jit();

		Results results = new Results(dirs);
		Parser p = new Parser(dirs);
		Timer timer = new Timer();
		timer.start();
		List<String> fails = p.parseHadoop(dirs.getHsfReduced().toFile());
		timer.stop();
		results.add("hadoop_sequence_file_reduced_mmtf", timer.get(), "ms");
		System.out.print("Reduced HSF fails: ");
		for (String s : fails) {
			System.out.print(s + " ");
		}
		System.out.println();
	}

	/**
	 * Runs the benchmark on the whole PDB measuring total time of parsing Hadoop sequence file
	 * (unzipped) and the times for entries in individual MMTF, PDB and mmCIF files.
	 */
	public void benchmarkWholeDatabase() throws IOException {
		Timer timer;
		Counter counter;
		Parser p = new Parser(dirs);
		DatasetGenerator d = new DatasetGenerator(dirs);
		List<String> codes = d.getCodes();
		Results results = new Results(dirs);

		jit();

		benchmarkHadoopSequenceFiles();

		timer = new Timer();
		timer.start();
		List<String> fails = p.parseHadoop(dirs.getHsfReduced().toFile());
		timer.stop();
		results.add("hadoop_sequence_file_reduced_mmtf", timer.get(), "ms");
		System.out.print("Reduced HSF fails: ");
		for (String s : fails) {
			System.out.print(s + " ");
		}
		System.out.println();

		timer = new Timer();
		timer.start();
		p.parseHadoop(dirs.getHsfFull().toFile());
		timer.stop();
		results.add("hadoop_sequence_file_mmtf", timer.get(), "ms");

		counter = new Counter();
		timer = new Timer();
		timer.start();
		for (String c : codes) {
			p.parseMmtfToBiojava(c);
			counter.next();
		}
		timer.stop();
		results.add("all_mmtf", timer.get(), "ms");

		counter = new Counter();
		timer = new Timer();
		timer.start();
		for (String c : codes) {
			p.parsePdbToBiojava(c);
			counter.next();
		}
		timer.stop();
		results.add("all_pdb", timer.get(), "ms");

		counter = new Counter();
		timer = new Timer();
		timer.start();
		for (String c : codes) {
			p.parseCifToBiojava(c);
			counter.next();
		}
		timer.stop();
		results.add("all_cif", timer.get(), "ms");

		results.end();
	}

	/**
	 * Measures times of parsing of 1000 random PDB entries, then times for 100 entries of three
	 * characteristic sizes and finally the times for the largest entry.
	 */
	public void benchmarkSamples() throws IOException {
		String prefix = "/mmtf-benchmark/";
		String[][] datasets = {
			Lines.readResource(prefix + "sample_1000.gz"),
			Lines.readResource(prefix + "sample_25.csv.gz"),
			Lines.readResource(prefix + "sample_50.csv.gz"),
			Lines.readResource(prefix + "sample_75.csv.gz")
		};
		String[] names = {"sample_of_1000", "quantile_25", "median",
			"quantile_75"};
		Parser p = new Parser(dirs);
		Results results = new Results(dirs);

		System.out.println("Just-in-time compilation.");
		jit();

		for (int index = 0; index < names.length; index++) {
			System.out.println("Measuring " + names[index]);
			String[] lines = datasets[index];
			String[] codes = new String[lines.length];
			for (int i = 0; i < codes.length; i++) {
				codes[i] = lines[i].trim().substring(0, 4);
			}

			Timer timer = new Timer();
			timer.start();
			for (String code : codes) {
				p.parseMmtfToBiojava(code);
			}
			timer.stop();
			results.add(names[index] + "_mmtf", timer.get(), "ms");

			timer = new Timer();
			timer.start();
			for (String code : codes) {
				p.parsePdbToBiojava(code);
			}
			timer.stop();
			results.add(names[index] + "_pdb", timer.get(), "ms");

			timer = new Timer();
			timer.start();
			for (String code : codes) {
				p.parseCifToBiojava(code);
			}
			timer.stop();
			results.add(names[index] + "_cif", timer.get(), "ms");
		}

		for (String code : selectedCodes) {
			Timer timer = new Timer();
			timer.start();
			p.parseMmtfToBiojava(code);
			timer.stop();
			results.add("selected_mmtf_" + code, timer.get(), "ms");

			timer = new Timer();
			timer.start();
			p.parseMmtfReducedToBiojava(code);
			timer.stop();
			results.add("selected_mmtf_reduced_" + code, timer.get(), "ms");

			timer = new Timer();
			timer.start();
			p.parseCifToBiojava("3j3q");
			timer.stop();
			results.add("selected_cif", timer.get(), "ms");
		}

		results.end();

	}

	private void prepareFiles(FileType fileType, File codesFile) throws IOException {
		LineFile lf = new LineFile(codesFile);
		for (String line : lf.readLines()) {
			String code = line.trim().substring(0, 4);
			dirs.prepareBatch(code, fileType, codesFile.getName());
		}
	}

	public void run(Set<String> flags) throws IOException {
		if (flags.contains("prepare_files_for_javascript")) { // for NGL
			for (FileType fileType : FileType.values()) {
				prepareFiles(fileType, dirs.getSample1000());
				prepareFiles(fileType, dirs.getSampleSmallest());
				prepareFiles(fileType, dirs.getSample25());
				prepareFiles(fileType, dirs.getSample50());
				prepareFiles(fileType, dirs.getSample75());
			}
		} else if (flags.contains("hsf")) {
			if (flags.contains("download")) {
				System.out.println("Downloading the whole PDB in Hadoop Sequence "
					+ "File format, it may take 30 minutes or more.");
				downloadHadoopSequenceFiles();
			}
			benchmarkHadoopSequenceFiles();
		} else if (flags.contains("full")) {
			System.out.println("Measuring parsing time on the whole PDB, this "
				+ "can take about 9 hours without time to download files "
				+ "(files are downloaded only if optional parameter "
				+ "\"download\" is provided).");
			if (flags.contains("download")) {
				System.out.println("Starting to download the whole PDB in MMTF,"
					+ "PDB and mmCIF file formats, total size is about 80 GB.");
				downloadWholeDatabase();
				transformHsf();
			}
			benchmarkWholeDatabase();
		} else if (flags.contains("sample")) {
			DatasetGenerator d = new DatasetGenerator(dirs);
			d.downloadSelected(selectedCodes);
			benchmarkSamples();
		} else {
			System.out.println("Generating samples of PDB codes.");
			DatasetGenerator d = new DatasetGenerator(dirs);
			d.generateSample(1000);
			QuantileSamples qs = new QuantileSamples(dirs);
			qs.generateDatasets(100);
			d.downloadSelected(selectedCodes);
			benchmarkSamples();
		}
	}

	/**
	 * Does some parsing before measurements, so that the first measurement is not at disadvantage
	 * due to Just In Time compilation.
	 */
	private void jit() {
		Parser p = new Parser(dirs);
		DatasetGenerator d = new DatasetGenerator(dirs);
		List<String> allCodes = d.getCodes();
		Random random = new Random(2); // 2 to work with different structures
		for (int i = 0; i < 100; i++) {
			try {
				p.parseMmtfToBiojava(allCodes.get(random.nextInt(allCodes.size())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {
				p.parsePdbToBiojava(allCodes.get(random.nextInt(allCodes.size())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			try {
				p.parseCifToBiojava(allCodes.get(random.nextInt(allCodes.size())));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Please provide a path to the directory where"
				+ "the data should be stored.");
			System.exit(1);
		}
		Benchmark b = new Benchmark(args[0]);
		Set<String> flags = new HashSet<>();
		for (int i = 1; i < args.length; i++) {
			flags.add(args[i].toLowerCase());
		}
		b.run(flags);
	}

}
