package Analysis.SoilAnalysis;

import Analysis.LabAnalysisResults.LabAnalysisResultDao;
import Analysis.LabAnalysisResults.LabAnalysisResultDaoImpl;
import Analysis.LabAnalysisResults.SoilLabAnalysisResult;
import DB.Dao.Dao;
import DB.Dao.ExtractionMethodDao;
import DB.DaoImpl.ExtractionMethodDaoImpl;
import DB.DaoImpl.SoilDaoImpl;
import DB.DaoImpl.layer_depth_typeDaoImpl;
import DB.Entites.ExtractionMethod;
import DB.Entites.Soil;
import DB.Entites.layer_depth_type;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * responsible for extracting from an excel file the necessary info about
 * the soil analysis lab results.
 */
public class ERSoilAnalysis {
    String path;
    List<ExtractionMethod> emList;

    /**
     * creates the ERSoilAnalysis, by receiving the file's path.
     * @param path - the path of the excel file.
     */
    public ERSoilAnalysis(String path) {
        this.path = path;
    }

    /**
     * reads the excel file, and creates the soil analysis, and the lab soil results.
     * @return the soil analysis id, that is used for both the 'soil_lab_analysis' table
     *         and the 'lab_analysis_results' table in the db.
     */
    public int read() {
        System.out.println("read read read!!!");
        Integer soilAnalysisId = null;
        SoilAnalysis soilAnalysis = null;
        List<SoilLabAnalysisResult> labAnalysisResultList = new ArrayList<>();
        try {
            //Create the input stream from the xlsx/xls file
            FileInputStream fis = new FileInputStream(path);

            //Create Workbook instance for xlsx/xls file input stream
            Workbook workbook = null;
            if (path.toLowerCase().endsWith("xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (path.toLowerCase().endsWith("xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            //Get the 1th sheet from the workbook
            Sheet firstSheet = workbook.getSheetAt(0);

            if (firstSheet.getRow(0).getCell(1).getStringCellValue().equals("Value") &&
                    firstSheet.getRow(1).getCell(1).getStringCellValue().equals("Soil")){

                String sample_name = firstSheet.getRow(2).getCell(1).getStringCellValue();
                int soil_analysis_id = (int)firstSheet.getRow(3).getCell(1).getNumericCellValue();
                boolean is_active = this.getIsActive(firstSheet.getRow(4).getCell(1).getStringCellValue());
                int farm_id = (int)firstSheet.getRow(5).getCell(1).getNumericCellValue();
                LocalDate sample_date = this.getLocalDate(firstSheet.getRow(6).getCell(1));
                int lab_id = (int)firstSheet.getRow(7).getCell(1).getNumericCellValue();
                int soil_type_id = this.getSoilTypeId(firstSheet.getRow(8).getCell(1).getStringCellValue());
                int layer_depth_id = this.getLayerDepthId(firstSheet.getRow(9).getCell(1).getStringCellValue());
                int irrigation_block_id = (int)firstSheet.getRow(10).getCell(1).getNumericCellValue();
                double organic_matter = firstSheet.getRow(11).getCell(1).getNumericCellValue();
                Double bulk_density = firstSheet.getRow(12).getCell(1).getNumericCellValue();
                Double soil_pH = firstSheet.getRow(14).getCell(1).getNumericCellValue();
                double soil_EC = firstSheet.getRow(15).getCell(1).getNumericCellValue();
                Double soil_CEC = firstSheet.getRow(16).getCell(1).getNumericCellValue();


                try{
                    soilAnalysis = new SoilAnalysis(sample_name,soil_analysis_id,is_active,farm_id,sample_date,lab_id,
                            soil_type_id,layer_depth_id,irrigation_block_id,organic_matter,bulk_density,soil_pH,
                            soil_EC,soil_CEC);
                    //insert to soil_lab_analysis schema
                    SoilAnalysisDao sad = new SoilAnalysisDaoImpl();
                    sad.insert(soilAnalysis);
                    soilAnalysisId = soil_analysis_id;
                } catch (Exception e){
                    e.printStackTrace();
                    System.out.println("failed to create water analysis");
                }
                //tries to read the values from the excel file.
                try {
                    /*
                     * READ PARAMETERS
                     */
                    double p1Val = firstSheet.getRow(17).getCell(1).getNumericCellValue();
                    double p13Val = firstSheet.getRow(18).getCell(1).getNumericCellValue();
                    double p14Val = firstSheet.getRow(19).getCell(1).getNumericCellValue();
                    double p20Val = firstSheet.getRow(20).getCell(1).getNumericCellValue();
                    double p2Val = firstSheet.getRow(21).getCell(1).getNumericCellValue();
                    double p3Val = firstSheet.getRow(22).getCell(1).getNumericCellValue();
                    double p4Val = firstSheet.getRow(23).getCell(1).getNumericCellValue();
                    double p5Val = firstSheet.getRow(24).getCell(1).getNumericCellValue();
                    double p6Val = firstSheet.getRow(25).getCell(1).getNumericCellValue();
                    double p7Val = firstSheet.getRow(26).getCell(1).getNumericCellValue();
                    double p8Val = firstSheet.getRow(27).getCell(1).getNumericCellValue();
                    double p9Val = firstSheet.getRow(28).getCell(1).getNumericCellValue();
                    double p10Val = firstSheet.getRow(29).getCell(1).getNumericCellValue();
                    double p11Val = firstSheet.getRow(30).getCell(1).getNumericCellValue();
                    double p12Val = firstSheet.getRow(31).getCell(1).getNumericCellValue();
                    double p16Val = firstSheet.getRow(32).getCell(1).getNumericCellValue();
                    double p17Val = firstSheet.getRow(33).getCell(1).getNumericCellValue();
                    double p19Val = firstSheet.getRow(34).getCell(1).getNumericCellValue();


                    int p1ExtractionMethod = this.getExtractionMethodId(firstSheet.getRow(17).getCell(3).getStringCellValue());
                    int p13ExtractionMethod = this.getExtractionMethodId(firstSheet.getRow(18).getCell(3).getStringCellValue());
                    int p14ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(19).getCell(3).getStringCellValue());
                    int p20ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(20).getCell(3).getStringCellValue());
                    int p2ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(21).getCell(3).getStringCellValue());
                    int p3ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(22).getCell(3).getStringCellValue());
                    int p4ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(23).getCell(3).getStringCellValue());
                    int p5ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(24).getCell(3).getStringCellValue());
                    int p6ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(25).getCell(3).getStringCellValue());
                    int p7ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(26).getCell(3).getStringCellValue());
                    int p8ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(27).getCell(3).getStringCellValue());
                    int p9ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(28).getCell(3).getStringCellValue());
                    int p10ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(29).getCell(3).getStringCellValue());
                    int p11ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(30).getCell(3).getStringCellValue());
                    int p12ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(31).getCell(3).getStringCellValue());
                    int p16ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(32).getCell(3).getStringCellValue());
                    int p17ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(33).getCell(3).getStringCellValue());
                    int p19ExtractionMethod  = this.getExtractionMethodId(firstSheet.getRow(34).getCell(3).getStringCellValue());

                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,1,p1Val,p1ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,2,p2Val,p2ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,3,p3Val,p3ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,4,p4Val,p4ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,5,p5Val,p5ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,6,p6Val,p6ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,7,p7Val,p7ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,8,p8Val,p8ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,9,p9Val,p9ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,10,p10Val,p10ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,11,p11Val,p11ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,12,p12Val,p12ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,13,p13Val,p13ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,14,p14Val,p14ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,16,p16Val,p16ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,17,p17Val,p17ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,19,p19Val,p19ExtractionMethod));
                    labAnalysisResultList.add(new SoilLabAnalysisResult(soil_analysis_id,20,p20Val,p20ExtractionMethod));
                    System.out.println("in the end");

                    //insert to lab_analysis_results schema
                    LabAnalysisResultDao larDao = new LabAnalysisResultDaoImpl();
                    larDao.insertAllSoil(labAnalysisResultList);

                } catch (Exception e){
                    e.printStackTrace();
                    System.out.println("wrong read parameters - lab analysis");
                }

            }
            else {
                System.out.println("wrong 2 row selected");
            }
            //close file input stream
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return soilAnalysisId;
    }

    /**
     * receives a cell to read from, in order to read the date of the sample
     * in the excel file.
     * @param cell - the cell to read from.
     * @return the date, converted to LocalDate, or null if not a date/not in the correct format.
     */
    LocalDate getLocalDate(Cell cell){
        if (DateUtil.isCellDateFormatted(cell)){
            Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
            return date.toInstant().atZone((ZoneId.systemDefault())).toLocalDate();
            //System.out.println(new SimpleDateFormat("dd/MM/yyyy").format(date));
        }
        System.out.println("problem at getLocalDate");
        return null;
    }

    /**
     * reades the "active" status from the excel, and true or false based on the given value.
     * @param s - the string to check.
     * @return true(if active=true), otherwise false.
     */
    Boolean getIsActive(String s){
        if(s.toLowerCase().equals("yes") || s.toLowerCase().equals("true")) {
            return true;
        }else if(s.toLowerCase().equals("no") || s.toLowerCase().equals("false")) {
            return false;
        }else {
            System.out.println("wrong in_active filed - water analysis");
        }
        return false;
    }

    /**
     * receives the string given for the soil in the excel file, and tries
     * to find matching soil name in the db.
     * @param soilName - the soil name from the excel file.
     * @return the soil's id (if a valid soil name was given), -1 otherwise
     */
    int getSoilTypeId(String soilName){
        Dao<Soil> soilDao = new SoilDaoImpl();
        List<Soil> soils = soilDao.selectAll();
        for (Soil soil : soils){
            if(soil.getName().toLowerCase().equals(soilName.toLowerCase())){
                return soil.getId();
            }
        }
        System.out.println("problem at getSoilTypeId");
        return -1;
    }

    /**
     * receives a layer depth range in string format, and tries to find
     * the same range in the db, in order to get the id for that range.
     * @param layerDepth - the layerDepth range string in the excel file.
     * @return the id that represents that range.
     */
    int getLayerDepthId(String layerDepth){
        Dao<layer_depth_type> ldtDao = new layer_depth_typeDaoImpl();
        List<layer_depth_type> layers = ldtDao.selectAll();
        for (layer_depth_type layer : layers){
            if(layer.getLayer_depth_name().toLowerCase().equals(layerDepth.toLowerCase())){
                return  layer.getLayer_depth_id();
            }
        }
        System.out.println("problem at getLayerDepthId");
        return -1;

    }

    /**
     * receives a string with the extraction method for a parameter,
     * and tries to find the same extraction method in the db, in order
     * to get the id for that extraction method.
     * @param s - the extraction method string.
     * @return the id for that extraction method.
     */
    private int getExtractionMethodId(String s) {
        if (this.emList == null) {
            ExtractionMethodDao emd = new ExtractionMethodDaoImpl();
            this.emList = emd.selectAll();
        }
        for (ExtractionMethod em : emList){
            if (s.toLowerCase().equals(em.getExtraction_method_desc().toLowerCase())){
                return em.getExtraction_method_id();
            }
        }
        if (!s.equals("")){
            System.out.println("Extraction method ID - problem : " + s);
        }
        return 0;
    }


}
