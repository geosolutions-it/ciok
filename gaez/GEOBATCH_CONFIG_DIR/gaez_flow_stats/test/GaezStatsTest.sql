UPDATE gaezprj.data 
SET status_code='TOK', status_msg='' 
WHERE  gaez_id='act2000_i_oce_2000_prd' or gaez_id='act2000_i_rt2_2000_prd'or gaez_id='act2000_i_sfl_2000_prd'or gaez_id='act2000_i_sfl_2000_yld' 
or gaez_id='act2000_r_oce_2000_prd' or gaez_id='act2000_r_rt1_2000_prd'or gaez_id='act2000_t_cc1_2000_prd'or gaez_id='act2000_t_olv_2000_prd' 
or gaez_id='act2000_t_sfl_2000_prd' or gaez_id='act2000_t_veg_2000_prd'or gaez_id= 'res02_eha22020i_oats125b_wde' or
gaez_id= 'res02_eha22020i_ricd125b_wde'or gaez_id='res02_eha22020i_spot125b_wde'or gaez_id='res02_eha22020i_srye015b_wde' or gaez_id='res02_eha22020i_srye015b_wde' 
or gaez_id='res02_eha22020i_sugb150b_wde' or gaez_id='res01_hist_lt2_1979' or gaez_id='res01_hist_lt3_1984' or gaez_id='res01_hist_prc_1978' 
or gaez_id='res01_hist_prc_1979' or gaez_id='res01_hist_prc_1990' or gaez_id='res01_mc2_c2a22020'or gaez_id='res01_mc2_c2a22080'
or gaez_id='res01_mc2_crav6190'or gaez_id='res01_mc2_csa22080'or gaez_id='res01_mc2_csb12020'or gaez_id='res01_mc2_csb12050'
or gaez_id='res01_mc2_csb12080'or gaez_id='res01_mc2_csb22050'or gaez_id='res01_mc2_eha22020'or gaez_id='res01_mc2_eha22080'
or gaez_id='res01_mc2_h3a12050'or gaez_id='res01_mc2_h3a22080'or gaez_id='res01_mc2_h3b22080'or gaez_id='res01_mcl_c2a22020'
or gaez_id='res01_mcl_c2a22050'or gaez_id='res01_mcl_c2a22080'or gaez_id='res01_mcl_c2b22020'or gaez_id='res01_mcl_csb12050'
or gaez_id='res01_mcl_csb12080'or gaez_id='res01_mcl_csb22050'or gaez_id='res01_mcl_csb22080'or gaez_id='res01_mcl_eha22020'
or gaez_id='res01_mcl_eha22050'or gaez_id='res01_mcl_eha22080'or gaez_id='res01_mcl_ehb22020'or gaez_id='res01_mcl_h3a22050'
	
SELECT * FROM gaez.data 
WHERE gaez_id='act2000_i_oce_2000_prd' or gaez_id='act2000_i_rt2_2000_prd'or gaez_id='act2000_i_sfl_2000_prd'or gaez_id='act2000_i_sfl_2000_yld' 
or gaez_id='act2000_r_oce_2000_prd' or gaez_id='act2000_r_rt1_2000_prd'or gaez_id='act2000_t_cc1_2000_prd'or gaez_id='act2000_t_olv_2000_prd' 
or gaez_id='act2000_t_sfl_2000_prd' or gaez_id='act2000_t_veg_2000_prd'or gaez_id= 'res02_eha22020i_oats125b_wde' or
gaez_id= 'res02_eha22020i_ricd125b_wde'or gaez_id='res02_eha22020i_spot125b_wde'or gaez_id='res02_eha22020i_srye015b_wde' or gaez_id='res02_eha22020i_srye015b_wde' 
or gaez_id='res02_eha22020i_sugb150b_wde' or gaez_id='res01_hist_lt2_1979' or gaez_id='res01_hist_lt3_1984' or gaez_id='res01_hist_prc_1978' 
or gaez_id='res01_hist_prc_1979' or gaez_id='res01_hist_prc_1990' or gaez_id='res01_mc2_c2a22020'or gaez_id='res01_mc2_c2a22080'
or gaez_id='res01_mc2_crav6190'or gaez_id='res01_mc2_csa22080'or gaez_id='res01_mc2_csb12020'or gaez_id='res01_mc2_csb12050'
or gaez_id='res01_mc2_csb12080'or gaez_id='res01_mc2_csb22050'or gaez_id='res01_mc2_eha22020'or gaez_id='res01_mc2_eha22080'
or gaez_id='res01_mc2_h3a12050'or gaez_id='res01_mc2_h3a22080'or gaez_id='res01_mc2_h3b22080'or gaez_id='res01_mcl_c2a22020'
or gaez_id='res01_mcl_c2a22050'or gaez_id='res01_mcl_c2a22080'or gaez_id='res01_mcl_c2b22020'or gaez_id='res01_mcl_csb12050'
or gaez_id='res01_mcl_csb12080'or gaez_id='res01_mcl_csb22050'or gaez_id='res01_mcl_csb22080'or gaez_id='res01_mcl_eha22020'
or gaez_id='res01_mcl_eha22050'or gaez_id='res01_mcl_eha22080'or gaez_id='res01_mcl_ehb22020'or gaez_id='res01_mcl_h3a22050'
ORDER BY status_code;

UPDATE gaezprj.data 
SET status_code='TOK', status_msg='' 
WHERE  file_name_tif='act2000_i_oce_2000_prd.tif' or file_name_tif='act2000_i_rt2_2000_prd.tif'or file_name_tif='act2000_i_sfl_2000_prd.tif'or file_name_tif='act2000_i_sfl_2000_yld.tif' 
or file_name_tif='act2000_r_oce_2000_prd.tif' or file_name_tif='act2000_r_rt1_2000_prd.tif'or file_name_tif='act2000_t_cc1_2000_prd.tif'or file_name_tif='act2000_t_olv_2000_prd.tif' 
or file_name_tif='act2000_t_sfl_2000_prd.tif' or file_name_tif='act2000_t_veg_2000_prd.tif'or file_name_tif= 'res02_eha22020i_oats125b_wde.tif' or
file_name_tif= 'res02_eha22020i_ricd125b_wde.tif'or file_name_tif='res02_eha22020i_spot125b_wde.tif'or file_name_tif='res02_eha22020i_srye015b_wde.tif' or file_name_tif='res02_eha22020i_srye015b_wde.tif' 
or file_name_tif='res02_eha22020i_sugb150b_wde.tif' or file_name_tif='res01_hist_lt2_1979.tif' or file_name_tif='res01_hist_lt3_1984.tif' or file_name_tif='res01_hist_prc_1978.tif' 
or file_name_tif='res01_hist_prc_1979.tif' or file_name_tif='res01_hist_prc_1990.tif' or file_name_tif='res01_mc2_c2a22020.tif'or file_name_tif='res01_mc2_c2a22080.tif'
or file_name_tif='res01_mc2_crav6190.tif'or file_name_tif='res01_mc2_csa22080.tif'or file_name_tif='res01_mc2_csb12020.tif'or file_name_tif='res01_mc2_csb12050.tif'
or file_name_tif='res01_mc2_csb12080.tif'or file_name_tif='res01_mc2_csb22050.tif'or file_name_tif='res01_mc2_eha22020.tif'or file_name_tif='res01_mc2_eha22080.tif'
or file_name_tif='res01_mc2_h3a12050.tif'or file_name_tif='res01_mc2_h3a22080.tif'or file_name_tif='res01_mc2_h3b22080.tif'or file_name_tif='res01_mcl_c2a22020.tif'
or file_name_tif='res01_mcl_c2a22050.tif'or file_name_tif='res01_mcl_c2a22080.tif'or file_name_tif='res01_mcl_c2b22020.tif'or file_name_tif='res01_mcl_csb12050.tif'
or file_name_tif='res01_mcl_csb12080.tif'or file_name_tif='res01_mcl_csb22050.tif'or file_name_tif='res01_mcl_csb22080.tif'or file_name_tif='res01_mcl_eha22020.tif'
or file_name_tif='res01_mcl_eha22050.tif'or file_name_tif='res01_mcl_eha22080.tif'or file_name_tif='res01_mcl_ehb22020.tif'or file_name_tif='res01_mcl_h3a22050.tif'

SELECT * 
FROM gaezprj.data 
WHERE file_name_tif='act2000_i_oce_2000_prd.tif' or file_name_tif='act2000_i_rt2_2000_prd.tif'or file_name_tif='act2000_i_sfl_2000_prd.tif'or file_name_tif='act2000_i_sfl_2000_yld.tif' 
or file_name_tif='act2000_r_oce_2000_prd.tif' or file_name_tif='act2000_r_rt1_2000_prd.tif'or file_name_tif='act2000_t_cc1_2000_prd.tif'or file_name_tif='act2000_t_olv_2000_prd.tif' 
or file_name_tif='act2000_t_sfl_2000_prd.tif' or file_name_tif='act2000_t_veg_2000_prd.tif'or file_name_tif= 'res02_eha22020i_oats125b_wde.tif' or
file_name_tif= 'res02_eha22020i_ricd125b_wde.tif'or file_name_tif='res02_eha22020i_spot125b_wde.tif'or file_name_tif='res02_eha22020i_srye015b_wde.tif' or file_name_tif='res02_eha22020i_srye015b_wde.tif' 
or file_name_tif='res02_eha22020i_sugb150b_wde.tif' or file_name_tif='res01_hist_lt2_1979.tif' or file_name_tif='res01_hist_lt3_1984.tif' or file_name_tif='res01_hist_prc_1978.tif' 
or file_name_tif='res01_hist_prc_1979.tif' or file_name_tif='res01_hist_prc_1990.tif' or file_name_tif='res01_mc2_c2a22020.tif'or file_name_tif='res01_mc2_c2a22080.tif'
or file_name_tif='res01_mc2_crav6190.tif'or file_name_tif='res01_mc2_csa22080.tif'or file_name_tif='res01_mc2_csb12020.tif'or file_name_tif='res01_mc2_csb12050.tif'
or file_name_tif='res01_mc2_csb12080.tif'or file_name_tif='res01_mc2_csb22050.tif'or file_name_tif='res01_mc2_eha22020.tif'or file_name_tif='res01_mc2_eha22080.tif'
or file_name_tif='res01_mc2_h3a12050.tif'or file_name_tif='res01_mc2_h3a22080.tif'or file_name_tif='res01_mc2_h3b22080.tif'or file_name_tif='res01_mcl_c2a22020.tif'
or file_name_tif='res01_mcl_c2a22050.tif'or file_name_tif='res01_mcl_c2a22080.tif'or file_name_tif='res01_mcl_c2b22020.tif'or file_name_tif='res01_mcl_csb12050.tif'
or file_name_tif='res01_mcl_csb12080.tif'or file_name_tif='res01_mcl_csb22050.tif'or file_name_tif='res01_mcl_csb22080.tif'or file_name_tif='res01_mcl_eha22020.tif'
or file_name_tif='res01_mcl_eha22050.tif'or file_name_tif='res01_mcl_eha22080.tif'or file_name_tif='res01_mcl_ehb22020.tif'or file_name_tif='res01_mcl_h3a22050.tif'
ORDER BY status_code;
