/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package water.api

import org.apache.spark.SparkContext
import org.apache.spark.h2o.util.SharedSparkTestContext
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import water.api.RDDs._

/**
 * Test suite for RDDs handler
 */
@RunWith(classOf[JUnitRunner])
class RDDsHandlerSuite extends FunSuite with SharedSparkTestContext {

  override def createSparkContext: SparkContext = new SparkContext("local[*]", "test-local")

  test("RDDsHandler.list() method") {
    val rname = "Test"
    val rpart = 21
    val rdd = sc.parallelize(1 to 10, rpart).setName(rname).cache()

    val rddsHandler = new RDDsHandler(sc, hc)
    val result = rddsHandler.list(3, new RDDsV3)
    assert (result.rdds.length == 1, "Number of created and persisted RDDs should be 1")
    assert (result.rdds(0).name.equals(rname), "Name matches")
    assert (result.rdds(0).partitions == rpart, "Number of partitions matches")

  }

  test("RDDsHandler.getRDD() method"){
    //Create and persist RDD
    val rname = "Test"
    val rpart = 21
    val rdd = sc.parallelize(1 to 100,rpart).setName(rname).cache()

    val rddsHandler = new RDDsHandler(sc, hc)
    val rddReq = new RDDWithMsgV3
    rddReq.searched_rdd_id =  rdd.id

    val result = rddsHandler.getRDD(3,rddReq)
    assert (result.rdd.id == rdd.id, "Original ID and obtained ID should match")
    assert (result.rdd.name.equals(rname), "Name matches")
    assert (result.rdd.partitions == rpart, "Number of partitions matches")
    assert (result.msg.equals("OK"),"Status should be OK")
  }


  test("RDDsHandler.toH2OFrame() method simple type"){
    //Create and persist RDD
    val rname = "Test"
    val rpart = 21

    val rdd = sc.parallelize(1 to 100,rpart).setName(rname).cache()

    val rddsHandler = new RDDsHandler(sc, hc)
    val rddReq = new RDD2H2OFrameIDV3
    rddReq.rdd_id =  rdd.id

    val result = rddsHandler.toH2OFrame(3,rddReq)
    val h2oframe = hc.asH2OFrame(result.h2oframe_id)
    assert (result.msg.equals("Success"),"Status should be Success")
    assert (h2oframe.numCols() == 1, "Number of columns should match")
    assert (h2oframe.names().sameElements(Seq("values")),"Column names should match")
    assert (h2oframe.numRows() == rdd.count(), "Number of rows should match")
  }

  test("RDDsHandler.toH2OFrame() method - product class"){
    //Create and persist RDD
    val rname = "Test"
    val rpart = 21

    val rdd = sc.parallelize(Seq(A(1,"A"),A(2,"B"),A(3,"C")),rpart).setName(rname).cache()

    val rddsHandler = new RDDsHandler(sc, hc)
    val rddReq = new RDD2H2OFrameIDV3
    rddReq.rdd_id =  rdd.id

    val result = rddsHandler.toH2OFrame(3,rddReq)
    val h2oframe = hc.asH2OFrame(result.h2oframe_id)
    assert (result.msg.equals("Success"),"Status should be Success")
    assert (h2oframe.numCols() == 2, "Number of columns should match")
    assert (h2oframe.names().sortWith((x, y) => x < y).sameElements(Seq("f0","f1")),"Column names should match")
    assert (h2oframe.numRows() == rdd.count(), "Number of rows should match")
  }

  test("RDDsHandler.getRDD() method, querying non-existing RDD"){
    val rddsHandler = new RDDsHandler(sc,hc)

    val rddReq = new RDDWithMsgV3
    rddReq.searched_rdd_id =  0

    val result = rddsHandler.getRDD(3,rddReq)
    assert (result.rdd == null, "Returned RDD should be null")
    assert (!result.msg.equals("OK"),"Status is not OK - it is message saying that given rdd does not exist")
  }
}
