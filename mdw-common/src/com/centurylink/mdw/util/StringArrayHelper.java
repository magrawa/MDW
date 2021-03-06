/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.util;


import java.util.StringTokenizer;

/**
 * Class that provides the helper operations for string Arrays
 */

public final class StringArrayHelper{    // CLASS VARIABLES --------------------------------------------

    // INSTANCE VARIABLES ------------------------------------------

    // CONSTRUCTORS ------------------------------------------------

    // PUBLIC AND PROTECTED  ------------------------------------------------

    /**
     * Method that checks if the passed in StringArr is empty
     * @param pArr
     * @return boolean result
     */
    public static boolean isEmpty(String[] pArr){
      if(pArr == null || pArr.length == 0)
         return true;
      return false;
    }
     /**
     * Method that checks if the passed in StringArr is NOT empty
     * @param pArr
     * @return boolean result
     */
    public static boolean isNotEmpty(String[] pArr){
      return !isEmpty(pArr);
    }

     /**
     * Method that converts the passed in String into array
     *
     * @param pStr
     * @param pDelimeiter
     * @return String Array
     */
    public static String[] covertToArray(String pStr, String pDelimiter){
      String[] arr = null;
      if(StringHelper.isEmpty(pStr) || StringHelper.isEmpty(pDelimiter)){
        return arr;
      }
      StringTokenizer tokenizer = new StringTokenizer(pStr, pDelimiter);
      int index = 0;
      arr = new String[tokenizer.countTokens()];
      while(tokenizer.hasMoreTokens()){
        arr[index] = tokenizer.nextToken();
        index ++;
      }
      return arr;
    }

     /**
     * Method that converts the passed in String into array
     *
     * @param pStr
     * @param pDelimeiter
     * @return String Array
     */
    public static String covertFromArray(String[] pStrArr, String pDelimiter){

      if(isEmpty(pStrArr) || StringHelper.isEmpty(pDelimiter)){
        return null;
      }
      StringBuffer buff = new StringBuffer();
      for(int i=0; i< pStrArr.length; i++){
        buff.append(pStrArr[i]).append(pDelimiter);

      }
      return buff.toString().substring(0, buff.toString().length() - 1);
    }

}
