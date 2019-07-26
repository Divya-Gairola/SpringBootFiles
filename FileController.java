package com.ctol.bench.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.ctol.bench.model.ExcelData;
import com.ctol.bench.model.ExcelHeader;
import com.ctol.bench.model.MetaData;

import com.ctol.bench.payload.UploadFileResponse;
import com.ctol.bench.repositories.ExcelRepository;
import com.ctol.bench.repositories.HeaderRepository;
import com.ctol.bench.repositories.MetaRepository;

import com.ctol.bench.service.FileStorageService;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCursor;

@RestController
@CrossOrigin(origins = "*")
public class FileController {

	private String fileName, fileDownloadUri, finalFile;
	@Autowired
	private FileStorageService fileStorageService;
	@Autowired
	ExcelRepository repository;

	@Autowired
	MetaRepository metaRepo;

	@Autowired
	HeaderRepository headerRepo;

//	@Autowired
//	MetaRepository metaRepo;

	@Autowired
	MongoTemplate mongoTemplate;

	@PostMapping("/uploadFile")
	public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
		fileName = fileStorageService.storeFile(file);

		fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/").path(fileName)
				.toUriString();

		finalFile = "C:\\Users\\" + System.getProperty("user.name") + "\\uploads\\".concat(fileName);

		String str = processExcel();
		extractExcel();
		if (str.equals("Success")) {
			return new UploadFileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize());
		} else {
			UploadFileResponse ur = new UploadFileResponse("FAILURE");
			return ur;
		}
	}

	@RequestMapping(value = "/query", method = RequestMethod.POST)
	public List<ExcelData> getResult(@RequestBody HashMap<String, List<String>> conditionMap,
			@RequestParam("page") int n) {
		Query query = new Query();

		Pageable pageableRequest;
		int k = n - 1;
		pageableRequest = PageRequest.of(k, 30);
		for (Map.Entry<String, List<String>> entry : conditionMap.entrySet()) {
			List<String> conditions = entry.getValue();
			query.addCriteria(Criteria.where("details." + entry.getKey()).in(conditions));
		}
		query.with(pageableRequest);
		List<ExcelData> excelRes = mongoTemplate.find(query, ExcelData.class);
		return excelRes;
	}

	@RequestMapping(value = "/no-of-query-records", method = RequestMethod.POST)
	public int getResultRecords(@RequestBody HashMap<String, List<String>> conditionMap) {
		Query query = new Query();

		Iterator mapIterator = conditionMap.entrySet().iterator();
		List<String> conditions = new ArrayList<>();
		while (mapIterator.hasNext()) {
			Map.Entry mapElement = (Map.Entry) mapIterator.next();
			conditions = (List<String>) mapElement.getValue();
			query.addCriteria(Criteria.where("details." + mapElement.getKey()).in(conditions));
		}
		List<ExcelData> excelRes = mongoTemplate.find(query, ExcelData.class);

		int noOfRecords = excelRes.size();

		return noOfRecords;
	}

	@RequestMapping(value = "/metadata", method = RequestMethod.GET)
	public List<MetaData> getMetaData() {
		return metaRepo.findAll();
	}

	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public List<ExcelData> extractExcel() {

		Workbook workbook = new XSSFWorkbook();
		Sheet excelSheet = workbook.createSheet("BenchWork");
		List<ExcelHeader> excelHeader = headerRepo.findAll();
		Row row = excelSheet.createRow(0);
		for (int i = 0; i < excelHeader.size(); i++) {
			row.createCell(i).setCellValue(excelHeader.get(i).getHeader());
		}
		int countRows = 1;
		List<ExcelData> excelData = repository.findAll();
		System.out.println(excelData.size());
	System.out.println(excelData);
	for(ExcelData oneRow : excelData)
		{
		Row row_num = excelSheet.createRow(countRows++);
			for(int i=0;i<excelHeader.size();i++)
			{
				row_num.createCell(i).setCellValue(oneRow.getDetails().get(excelHeader.get(i).getHeader()));
			}				
		}
		
		File file = new File("C:\\Users\\dgairola\\uploads\\excelFile.xlsx");
		OutputStream output = null;
		try
		{
			file.createNewFile();
			output = new FileOutputStream(file);
			workbook.write(output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return excelData;
	}
     
	@RequestMapping(value = "/getDB", method = RequestMethod.GET)
	public List<ExcelData> getAllDocs(@RequestParam("page") int n) {
		Pageable pageableRequest;
		int k = n - 1;
		pageableRequest = PageRequest.of(k, 30);
		Query query = new Query();
		query.with(pageableRequest);

		List<ExcelData> pagination = mongoTemplate.find(query, ExcelData.class);

		return pagination;
	}

	@RequestMapping(value = "/no-of-pages", method = RequestMethod.GET)
	public int geNoOfPages() {
		List<ExcelData> excelData = repository.findAll();

		return excelData.size();
	}

	// inserting column name into header table
	public List<String> populateHeader(Sheet datatypeSheet) {
		Iterator<Cell> iteratorHeader = datatypeSheet.getRow(datatypeSheet.getFirstRowNum()).iterator();

		List<String> headers = new ArrayList<String>();
		ExcelHeader excelHeader = new ExcelHeader();

		while (iteratorHeader.hasNext()) {
			Cell currentHeader = iteratorHeader.next();
			if (currentHeader.getCellType() == CellType.STRING) {
				headers.add(currentHeader.getStringCellValue());
				excelHeader.setHeader(currentHeader.getStringCellValue());

			} else if (currentHeader.getCellType() == CellType.NUMERIC) {
				headers.add("" + currentHeader.getNumericCellValue());
				excelHeader.setHeader("" + currentHeader.getNumericCellValue());
			}
			headerRepo.save(excelHeader);
		}
		return headers;

	}

	public String processExcel() throws Exception {
		/*
		 * Apache POI Debugging ClassLoader classloader =
		 * org.apache.poi.poifs.filesystem.POIFSFileSystem.class.getClassLoader(); URL
		 * res = classloader.getResource("org/apache/poi/util/POILogger.class"); String
		 * path = res.getPath(); return "POI came from " + path;
		 */

		FileInputStream excelFile = new FileInputStream(new File(finalFile));
		Workbook workbook = null;
		try {
			workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			List<String> headers = populateHeader(datatypeSheet);
			boolean skipHeader = true;
			for (Row row : datatypeSheet) {

				// TreeMap is being used for sorting the incoming dat so that it will be
				// iterated in a particular order
				Map<String, String> nameMap = new TreeMap<>(new Comparator<String>() {
					@Override
					public int compare(String name_one, String name_two) {
						return headers.indexOf(name_one) - headers.indexOf(name_two);
					}
				});

				if (skipHeader) {
					skipHeader = false;
					continue;
				}

				int lastColumn = headers.size();

				for (int cn = 0; cn < lastColumn; cn++) {
					Cell c = row.getCell(cn);
					String s = "" + c;
//					System.out.println(headers.get(cn)+" "+ s);
					nameMap.put(headers.get(cn), s);
				}

				ExcelData ex = new ExcelData();
				ex.set_id(row.getCell(0).toString());
				ex.setDetails(nameMap);
//				System.out.println(ex);
				repository.save(ex);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			excelFile.close();
			workbook.close();
		}

		loadMetaData();

		return "Success";
	}

	// function used for loading data into metadata table and the data from metadata
	// table is being used for filtering purpose
	public void loadMetaData() {

		List<String> header = new ArrayList<>();
		List<ExcelHeader> newRepo = headerRepo.findAll();

		for (ExcelHeader temp : newRepo) {
			header.add(temp.getHeader());
		}

		Map<String, List<String>> metadata = new HashMap<>();

		MetaData md = new MetaData();

		for (String headValue : header) {
			DistinctIterable<String> iterable = mongoTemplate.getCollection("excelData")
					.distinct("details.".concat(headValue), String.class);
			MongoCursor<String> cursor = iterable.iterator();
			List<String> list = new ArrayList<>();

			while (cursor.hasNext()) {
				list.add("" + cursor.next());
			}

			Collections.sort(list);
			metadata.put(headValue, list);

		}
		md.setMap(metadata);
		metaRepo.save(md);

	}
}