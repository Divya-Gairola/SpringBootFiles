package com.ctol.bench.repositories;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.ctol.bench.model.ExcelData;

public interface ExcelRepository extends MongoRepository<ExcelData, String> 
{
//	ExcelData findBy_id(ObjectId _id);	    
}
