










package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 书籍
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/shuji")
public class ShujiController {
    private static final Logger logger = LoggerFactory.getLogger(ShujiController.class);

    @Autowired
    private ShujiService shujiService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private ZuozheService zuozheService;

    @Autowired
    private YonghuService yonghuService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isEmpty(role))
            return R.error(511,"权限为空");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("作者".equals(role))
            params.put("zuozheId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = shujiService.queryPage(params);

        //字典表数据转换
        List<ShujiView> list =(List<ShujiView>)page.getList();
        for(ShujiView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShujiEntity shuji = shujiService.selectById(id);
        if(shuji !=null){
            //entity转view
            ShujiView view = new ShujiView();
            BeanUtils.copyProperties( shuji , view );//把实体数据重构到view中

                //级联表
                ZuozheEntity zuozhe = zuozheService.selectById(shuji.getZuozheId());
                if(zuozhe != null){
                    BeanUtils.copyProperties( zuozhe , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setZuozheId(zuozhe.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody ShujiEntity shuji, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,shuji:{}",this.getClass().getName(),shuji.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(StringUtil.isEmpty(role))
            return R.error(511,"权限为空");
        else if("作者".equals(role))
            shuji.setZuozheId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<ShujiEntity> queryWrapper = new EntityWrapper<ShujiEntity>()
            .eq("zuozhe_id", shuji.getZuozheId())
            .eq("shuji_name", shuji.getShujiName())
            .eq("shuji_types", shuji.getShujiTypes())
            .eq("shuji_clicknum", shuji.getShujiClicknum())
            .eq("shangxia_types", shuji.getShangxiaTypes())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShujiEntity shujiEntity = shujiService.selectOne(queryWrapper);
        if(shujiEntity==null){
            shuji.setShujiClicknum(1);
            shuji.setShangxiaTypes(1);
            shuji.setInsertTime(new Date());
            shuji.setCreateTime(new Date());
            shujiService.insert(shuji);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody ShujiEntity shuji, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,shuji:{}",this.getClass().getName(),shuji.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(StringUtil.isEmpty(role))
//            return R.error(511,"权限为空");
//        else if("作者".equals(role))
//            shuji.setZuozheId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<ShujiEntity> queryWrapper = new EntityWrapper<ShujiEntity>()
            .notIn("id",shuji.getId())
            .andNew()
            .eq("zuozhe_id", shuji.getZuozheId())
            .eq("shuji_name", shuji.getShujiName())
            .eq("shuji_types", shuji.getShujiTypes())
            .eq("shuji_clicknum", shuji.getShujiClicknum())
            .eq("shangxia_types", shuji.getShangxiaTypes())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShujiEntity shujiEntity = shujiService.selectOne(queryWrapper);
        if("".equals(shuji.getShujiPhoto()) || "null".equals(shuji.getShujiPhoto())){
                shuji.setShujiPhoto(null);
        }
        if(shujiEntity==null){
            //  String role = String.valueOf(request.getSession().getAttribute("role"));
            //  if("".equals(role)){
            //      shuji.set
            //  }
            shujiService.updateById(shuji);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        shujiService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }

    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        try {
            List<ShujiEntity> shujiList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            ShujiEntity shujiEntity = new ShujiEntity();
//                            shujiEntity.setZuozheId(Integer.valueOf(data.get(0)));   //作者 要改的
//                            shujiEntity.setShujiName(data.get(0));                    //书籍名称 要改的
//                            shujiEntity.setShujiPhoto("");//照片
//                            shujiEntity.setShujiTypes(Integer.valueOf(data.get(0)));   //书籍类型 要改的
//                            shujiEntity.setShujiClicknum(Integer.valueOf(data.get(0)));   //点击次数 要改的
//                            shujiEntity.setShangxiaTypes(Integer.valueOf(data.get(0)));   //是否上架 要改的
//                            shujiEntity.setShujiContent("");//照片
//                            shujiEntity.setInsertTime(date);//时间
//                            shujiEntity.setCreateTime(date);//时间
                            shujiList.add(shujiEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        shujiService.insertBatch(shujiList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        // 没有指定排序字段就默认id倒序
        if(StringUtil.isEmpty(String.valueOf(params.get("orderBy")))){
            params.put("orderBy","id");
        }
        PageUtils page = shujiService.queryPage(params);

        //字典表数据转换
        List<ShujiView> list =(List<ShujiView>)page.getList();
        for(ShujiView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        ShujiEntity shuji = shujiService.selectById(id);
            if(shuji !=null){

                //点击数量加1
                shuji.setShujiClicknum(shuji.getShujiClicknum()+1);
                shujiService.updateById(shuji);

                //entity转view
                ShujiView view = new ShujiView();
                BeanUtils.copyProperties( shuji , view );//把实体数据重构到view中

                //级联表
                    ZuozheEntity zuozhe = zuozheService.selectById(shuji.getZuozheId());
                if(zuozhe != null){
                    BeanUtils.copyProperties( zuozhe , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setZuozheId(zuozhe.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody ShujiEntity shuji, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,shuji:{}",this.getClass().getName(),shuji.toString());
        Wrapper<ShujiEntity> queryWrapper = new EntityWrapper<ShujiEntity>()
            .eq("zuozhe_id", shuji.getZuozheId())
            .eq("shuji_name", shuji.getShujiName())
            .eq("shuji_types", shuji.getShujiTypes())
            .eq("shuji_clicknum", shuji.getShujiClicknum())
            .eq("shangxia_types", shuji.getShangxiaTypes())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        ShujiEntity shujiEntity = shujiService.selectOne(queryWrapper);
        if(shujiEntity==null){
            shuji.setInsertTime(new Date());
            shuji.setCreateTime(new Date());
        //  String role = String.valueOf(request.getSession().getAttribute("role"));
        //  if("".equals(role)){
        //      shuji.set
        //  }
        shujiService.insert(shuji);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }


}
