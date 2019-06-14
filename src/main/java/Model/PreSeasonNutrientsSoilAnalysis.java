package Model;

import DB.Dao.Dao;
import DB.Dao.soil_thresholdsDao;
import DB.DaoImpl.layer_depth_typeDaoImpl;
import DB.DaoImpl.parametersDaoImpl;
import DB.DaoImpl.soil_thresholdsDaoImpl;
import DB.Entites.Soil;
import DB.Entites.layer_depth_type;
import DB.Entites.parameters;
import DB.Entites.soil_thresholds;
import Model.WriteOutput.NutrientsOutput;
import Model.WriteOutput.SoilAnalysisOutput;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * responsible for calculating all the necessary pre soil analysis data,
 * including the output for the interpretation table, and the recommendation
 * and soil correction values for the adjusted nutrients table.
 */
public class PreSeasonNutrientsSoilAnalysis {

    public PreSeasonNutrientsSoilAnalysis() {

    }

    /**
     * reads from the parameters and the db the necessary data needed for the pre season soil analysis
     * of the nutrients.
     * calculates the interpretation output table for the soil analysis, including soil recommendation
     * and correction, which are added to the adjusted nutrients output table.
     * @param p - the parameters data
     * @param n - the nutrients data
     * @return n - the updated nutrients data
     */
    public Nutrients PreSeasonNutrientsSoilAnalysis(Parameters p, Nutrients n) {
        double soilCorrectionFactor = p.getUi().getSelectedSoilCorrection();
        //read p factors from database
        List<List<Double>> factorListOfLists = factorsListInitialization();
        List<Double> veryLowFactor = factorListOfLists.get(0);
        List<Double> lowFactor = factorListOfLists.get(1);
        List<Double> highFactor = factorListOfLists.get(2);
        List<Double> veryHighFactor = factorListOfLists.get(3);
        List<Double> preLowFactor = factorListOfLists.get(4);
        List<Double> preHighFactor = factorListOfLists.get(5);

        //yes no activation if - to complete/check with ofer
        if (p.getSa().isIs_active()) {
            Soil soil = n.getSoil();
            int layerDepthId = p.getSa().getLayer_depth_id();
            Dao<layer_depth_type> ldtd = new layer_depth_typeDaoImpl();
            layer_depth_type ldt = ldtd.selectById(layerDepthId);
            double layerAvg = (double) (ldt.getLayer_max() - ldt.getLayer_min());
            double layerDepth = layerAvg / 100; // divide value (taken from DB) by 100
            double db;
            if (p.getSa().getBulk_density() != null) {
                db = 1000 * p.getSa().getBulk_density();
            } else {
                db = 1000 * n.getSoil().getDefualtBulkDensity(); //get default value
            }
            Double cec;
            if (p.getSa().getSoil_CEC() != null) {
                cec = p.getSa().getSoil_CEC();
            } else {
                cec = (double) soil.getDefualtCEC(); //default bulk density in matlab - seems like a mistake. why convert?
            }

            double soilWeight = db * layerDepth * 10000; //soil weight in kg/ha
            double wettedArea = p.getUi().getSelectedIrrigationMethod().getIrrigation_method_wetted_area();
            System.out.println("wetted area in pre analysis: " + wettedArea);

            //determine phenological stage
            Boolean preSeason;
            String stage;
            LocalDate userDate = p.getUi().getSelectedDate();
            if (userDate == null) {
                userDate = LocalDate.now();
            }
            LocalDate sampleDate = p.getSa().getSample_date();
            if (sampleDate.isBefore(userDate)) {
                stage = "pre-season";
                preSeason = true;
            } else {
                stage = p.getCropStages().get(0).stageName; //get the name of first stage of crop
                preSeason = false;
            }
            //load soil analysis thresholds from database (currently n threshold are taken from the same table)
            soil_thresholdsDao std = new soil_thresholdsDaoImpl();
            List<soil_thresholds> stList = std.selectAll();
            List<String> nutrientNames = Arrays.asList("N", "P", "K", "Ca", "Mg", "S", "Fe", "B", "Mn", "Zn", "Cu", "Mo");
            String[] interp = {"Very Low", "Low", "Sufficient", "High", "Very High"};

            List<Double> soilNutrientsResults = new ArrayList<Double>(Collections.nCopies(nutrientNames.size(), 0.0));
            List<String> analysisStatus = new ArrayList<String>(Collections.nCopies(nutrientNames.size(), null));
            List<String> soilThresholds = new ArrayList<String>(Collections.nCopies(nutrientNames.size(), null));
            List<Double> soilNutrientsBalance = new ArrayList<Double>(Collections.nCopies(nutrientNames.size(), 0.0));
            List<Double> soilRecommendation = new ArrayList<Double>(Collections.nCopies(nutrientNames.size(), 0.0));
            List<Double> soilCorrection = new ArrayList<Double>(Collections.nCopies(nutrientNames.size(), 0.0));

            Double nh4 = p.getSlar().get(12).getParameter_value(); //13th element value - can be done in the long way.
            Double n03 = p.getSlar().get(13).getParameter_value(); //14th element value - can be done in the long way.
            //double nOrganic = p.getSlar().get(19).getParameter_value(); //20th element value - can be done in the long way.
            Double nOrganic = 0.0; //unused in matlab code.
            Double nTotal = nh4 + n03; //skipped null check, since it does not appear in final soil analysis input
            System.out.println("n total is " + nTotal);

            Double result;
            if (nTotal == null) {
                result = nh4 + n03;
            } else {
                result = nTotal;
            }
            System.out.println("n result is: " + result);
            //find thresholds - for now, n tresholds found in soil thresholds, with no regard to stage (ofer)
            //that's why it's currently the same both in both instances
            soil_thresholds nSoilThresh;
            if (preSeason) {
                System.out.println("in preseasons?");
                nSoilThresh = stList.get(stList.size() - 1); //list containing n thresholds
                soilThresholds.set(0, String.valueOf(nSoilThresh.getLow_threshold()) + " - "
                        + String.valueOf(nSoilThresh.getHigh_threshold()));
            } else {
                nSoilThresh = stList.get(stList.size() - 1);
                soilThresholds.set(0, String.valueOf(nSoilThresh.getLow_threshold()) + " - "
                        + String.valueOf(nSoilThresh.getHigh_threshold()));
            }
            List<Double> nSoilThresholdsList = new ArrayList<>();
            nSoilThresholdsList.add(nSoilThresh.getVery_low_threshold());
            nSoilThresholdsList.add(nSoilThresh.getLow_threshold());
            nSoilThresholdsList.add(nSoilThresh.getTarget_value());
            nSoilThresholdsList.add(nSoilThresh.getHigh_threshold());
            nSoilThresholdsList.add(nSoilThresh.getVery_high_threshold());
            System.out.println("n thresholds is: " + nSoilThresholdsList);
            if (result == null) {
                soilRecommendation.set(0, 0.0);
                analysisStatus.set(0, "");
            } else {
                Integer nThreshIndex = null;
                //get the first index of which the n total value in lower than the threshold
                for (int i = 0; i < interp.length; i++) {
                    if (result < nSoilThresholdsList.get(i)) {
                        nThreshIndex = i;
                        break;
                    }
                }
                if (nThreshIndex == null) {
                    //very high
                    analysisStatus.set(0, interp[interp.length - 1]);
                } else {
                    //other status
                    analysisStatus.set(0, interp[nThreshIndex]);
                }
                System.out.println("n status is: " + analysisStatus.get(0));
                if (result == null) {
                    soilNutrientsBalance.set(0, null);
                } else {
                    Double balance = result - nSoilThresholdsList.get(2); //the target n
                    Double kg = (balance * soilWeight) / Math.pow(10, 6) * wettedArea;
                    soilNutrientsBalance.set(0, kg);
                }
                soilNutrientsResults.set(0, result);

                if (!analysisStatus.get(0).equals("Sufficient")) {
                    soilRecommendation.set(0, -soilNutrientsBalance.get(0));
                } else {
                    soilRecommendation.set(0, 0.0);
                }
            }
            //calculations for nutrients 2-12
            for (int i = 1; i < nutrientNames.size(); i++) {


                //get nutrient value from soil analysis lab results
                result = p.getSlar().get(i).getParameter_value();
                soil_thresholds currentThresh = stList.get(i - 1);
                List<Double> currentSoilThreshList = new ArrayList<>();
                //adding the current nutrient thresholds to the list
                currentSoilThreshList.add(currentThresh.getVery_low_threshold());
                currentSoilThreshList.add(currentThresh.getLow_threshold());
                currentSoilThreshList.add(currentThresh.getTarget_value());
                currentSoilThreshList.add(currentThresh.getHigh_threshold());
                currentSoilThreshList.add(currentThresh.getVery_high_threshold());
                //System.out.println("current thresh is: " +currentThresh);
                soilThresholds.set(i, currentThresh.getLow_threshold() + " - " + currentThresh.getHigh_threshold());
                Double target;
                Double factor;
                if (!nutrientNames.get(i).equals("K") && !nutrientNames.get(i).equals("Ca") && !nutrientNames.get(i).equals("Mg")) {
                    target = currentThresh.getTarget_value();
                    System.out.println(target);
                }
                //conversion if nutrient is K, CA or Mg
                //need to check with ofer from where to take that value (didn't ask to add original)
                else {
                    if (nutrientNames.get(i).equals("K")) {
                        factor = 10 / 0.0256;
                    } else if (nutrientNames.get(i).equals("Ca")) {
                        factor = 10 / 0.0499;
                    } else {
                        factor = 10 / 0.0823;
                    }
                    for (int j = 0; j < currentSoilThreshList.size(); j++) {
                        Double thresh = currentSoilThreshList.get(j);
                        currentSoilThreshList.set(j, thresh * factor * cec);
                    }
                    Double soilThresCur = currentSoilThreshList.get(1);
                    Double soilThresCur2 = currentSoilThreshList.get(3);
                    soilThresCur = new BigDecimal(soilThresCur).setScale(2, RoundingMode.FLOOR).doubleValue();
                    soilThresCur2 = new BigDecimal(soilThresCur2).setScale(2, RoundingMode.FLOOR).doubleValue();
                    soilThresholds.set(i, soilThresCur + " - " + soilThresCur2);
                    target = currentSoilThreshList.get(2);
                }

                if (result == null) {
                    analysisStatus.set(i, "");
                } else {
                    Integer threshIndex = null;
                    //get the first index of which the n total value in lower than the threshold
                    for (int j = 0; j < interp.length; j++) {
                        if (result < currentSoilThreshList.get(j)) {
                            threshIndex = j;
                            break;
                        }
                    }
                    if (threshIndex == null) {
                        analysisStatus.set(i, interp[interp.length - 1]);
                    } else {
                        analysisStatus.set(i, interp[threshIndex]);
                    }
                    //System.out.println("status is: " + analysisStatus.get(i));
                    if (result == null) {
                        soilNutrientsBalance.set(i, null);
                    } else {
                        Double balance = result - target; //the target
                        System.out.println("balance is " + balance);
                        Double kg = (balance * soilWeight) / Math.pow(10, 6) * wettedArea;
                        System.out.println("kg is " + kg);
                        soilNutrientsBalance.set(i, kg);
                    }
                    soilNutrientsResults.set(i, result);
                }

                if (nutrientNames.get(i).equals("P") || nutrientNames.get(i).equals("K") || nutrientNames.get(i).equals("Ca")
                        || nutrientNames.get(i).equals("Mg")) {
                    if (analysisStatus.get(i).equals("Very Low") || analysisStatus.get(i).equals("Low")) {
                        if (preSeason) {
                            soilCorrection.set(i, -soilNutrientsBalance.get(i) * soilCorrectionFactor);
                        } else {
                            if (analysisStatus.get(i).equals("Low")) {
                                soilRecommendation.set(i, -soilNutrientsBalance.get(i) * lowFactor.get(i));
                            } else {
                                soilRecommendation.set(i, -soilNutrientsBalance.get(i) * veryLowFactor.get(i));
                            }
                        }
                    } else if (analysisStatus.get(i).equals("High") || analysisStatus.get(i).equals("Very High")) {
                        if (preSeason) {
                            soilRecommendation.set(i, -soilNutrientsBalance.get(i) * preHighFactor.get(i));
                        } else {
                            if (analysisStatus.get(i).equals("High")) {
                                soilRecommendation.set(i, -soilNutrientsBalance.get(i) * highFactor.get(i));
                            } else {
                                soilRecommendation.set(i, -soilNutrientsBalance.get(i) * veryHighFactor.get(i));
                            }
                        }
                    } else {
                        soilRecommendation.set(i, 0.0);
                    }
                }
                if (nutrientNames.get(i).equals("N") || nutrientNames.get(i).equals("S")) {
                    if (!analysisStatus.get(i).equals("Sufficient")) {
                        soilRecommendation.set(i, -soilNutrientsBalance.get(i));
                    } else {
                        soilRecommendation.set(i, 0.0);
                    }
                }
                //System.out.println(currentSoilThreshList);
            }
            for (int i = 0; i < soilRecommendation.size(); i++) {
                Double currentValue = soilRecommendation.get(i);
                System.out.println("current value " + currentValue);
                currentValue = Double.parseDouble(String.format("%.0f", currentValue));
                soilRecommendation.set(i, currentValue);
            }

            double[] oxide = {1, 2.29, 1.2, 1.4, 1.6, 1, 1, 1, 1, 1, 1, 1};
            //multiply soil recommendation and correction by oxide values
            for (int i = 0; i < oxide.length; i++) {
                soilRecommendation.set(i, soilRecommendation.get(i) * oxide[i]);
                soilCorrection.set(i, soilCorrection.get(i) * oxide[i]);
            }
            soilNutrientsResults = decimalRound(soilNutrientsResults);
            soilNutrientsBalance = decimalRound(soilNutrientsBalance);
            soilRecommendation = decimalRound(soilRecommendation);
            soilCorrection = decimalRound(soilCorrection);
            List<SoilAnalysisOutput> soilAnalysisOutputList = new ArrayList<>();
            for (int i = 0; i < nutrientNames.size(); i++) {
                SoilAnalysisOutput sao = new SoilAnalysisOutput(nutrientNames.get(i), soilNutrientsResults.get(i),
                        analysisStatus.get(i), soilThresholds.get(i), soilNutrientsBalance.get(i),
                        soilRecommendation.get(i), soilCorrection.get(i));
                soilAnalysisOutputList.add(sao);
            }
            n.getPreSeason().setSoilAnalysis(soilAnalysisOutputList);
            //check - looks like recommendation row will never appear beforehand

            //add to nutrients output the recommendation and correction
            NutrientsOutput recommendationOutput = new NutrientsOutput("Recommendation", soilRecommendation);
            NutrientsOutput soilCorrectionOutput = new NutrientsOutput("Soil_Correction", soilCorrection);
            List<NutrientsOutput> nutrientsOutputsList = n.getPreSeason().getAdjNutrients();
            System.out.println("size before is " + nutrientsOutputsList.size());

            //adding to adjust nutrients table the rows recommendation and soil correction
            nutrientsOutputsList.add(recommendationOutput);
            nutrientsOutputsList.add(soilCorrectionOutput);
            n.getPreSeason().setAdjNutrients(nutrientsOutputsList);
            for (int i = 0; i < n.getPreSeason().getAdjNutrients().size(); i++) {
                System.out.println(n.getPreSeason().getAdjNutrients().get(i).getStageName() +
                        " " + n.getPreSeason().getAdjNutrients().get(i));
            }
            System.out.println("results are: " + soilNutrientsResults);
            System.out.println("analysis status is " + analysisStatus);
            System.out.println("soil thresholds is: " + soilThresholds);
            System.out.println("balance is " + soilNutrientsBalance);
            System.out.println("recommendation is: " + soilRecommendation);
            System.out.println("correction is: " + soilCorrection);
        }
        return n;
    }

    /**
     * creates a list of lists, the contains in each list factors values for each
     * nutrient. the factors are (very low, low, high, very high, pre low, pre high)
     * @return factorListofLists - a list of lists of p factors
     */
    public List<List<Double>> factorsListInitialization() {
        List<List<Double>> factorListOfLists = new ArrayList<>();
        Dao<parameters> pd = new parametersDaoImpl();
        List<parameters> parametersList = pd.selectAll();

        //creating all the lists to be added to the list of lists
        List<Double> veryLowFactor = new ArrayList<>();
        List<Double> lowFactor = new ArrayList<>();
        List<Double> highFactor = new ArrayList<>();
        List<Double> veryHighFactor = new ArrayList<>();
        List<Double> preLowFactor = new ArrayList<>();
        List<Double> preHighFactor = new ArrayList<>();

        //add for each list the factors values of each nutrient
        for (int i=0;i<parametersList.size();i++) {
            veryLowFactor.add(parametersList.get(i).getVery_low_factor());
            lowFactor.add(parametersList.get(i).getLow_factor());
            highFactor.add(parametersList.get(i).getHigh_factor());
            veryHighFactor.add(parametersList.get(i).getVery_high_factor());
            preLowFactor.add(parametersList.get(i).getPre_low_factor());
            preHighFactor.add(parametersList.get(i).getPre_high_factor());
        }
        //add each factor list to the list of lists
        factorListOfLists.add(veryLowFactor);
        factorListOfLists.add(lowFactor);
        factorListOfLists.add(highFactor);
        factorListOfLists.add(veryHighFactor);
        factorListOfLists.add(preLowFactor);
        factorListOfLists.add(preHighFactor);

        return factorListOfLists;
    }
        public List<Double> decimalRound(List<Double> row) {
            for (int i=0;i<row.size();i++) {
                Double d = new BigDecimal(row.get(i)).setScale(2, RoundingMode.FLOOR).doubleValue();
                row.set(i,d);
            }
            return row;
        }

}
