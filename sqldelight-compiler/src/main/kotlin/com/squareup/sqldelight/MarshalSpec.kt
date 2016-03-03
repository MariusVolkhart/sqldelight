/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.sqldelight

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.sqldelight.model.Type.BOOLEAN
import com.squareup.sqldelight.model.Type.ENUM
import com.squareup.sqldelight.model.adapterField
import com.squareup.sqldelight.model.adapterType
import com.squareup.sqldelight.model.constantName
import com.squareup.sqldelight.model.isHandledType
import com.squareup.sqldelight.model.isNullable
import com.squareup.sqldelight.model.javaType
import com.squareup.sqldelight.model.marshaledValue
import com.squareup.sqldelight.model.methodName
import com.squareup.sqldelight.model.type
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

class MarshalSpec(
    private val table: SqliteParser.Create_table_stmtContext,
    private val interfaceClassName: ClassName,
    private val fileName: String
) {
  private val marshalClassName = interfaceClassName.nestedClass("${fileName}Marshal")

  internal fun build(): TypeSpec {
    val marshal = TypeSpec.classBuilder(marshalClassName.simpleName())
        .addModifiers(PUBLIC, STATIC)
        .addTypeVariable(TypeVariableName.get("T",
            ParameterizedTypeName.get(marshalClassName, TypeVariableName.get("T"))))

    marshal
        .addField(FieldSpec.builder(CONTENTVALUES_TYPE, CONTENTVALUES_FIELD, PROTECTED)
            .initializer("new \$T()", CONTENTVALUES_TYPE)
            .build())
        .addMethod(MethodSpec.methodBuilder(CONTENTVALUES_METHOD)
            .addModifiers(PUBLIC, FINAL)
            .returns(CONTENTVALUES_TYPE)
            .addStatement("return $CONTENTVALUES_FIELD")
            .build())

    val copyConstructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)
    copyConstructor.addParameter(interfaceClassName, "copy");

    val constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC)

    for (column in table.column_def()) {
      if (column.isHandledType) {
        marshal.addMethod(marshalMethod(column))
      } else {
        marshal.addField(column.adapterType(), column.adapterField(), PRIVATE, FINAL)
        constructor.addParameter(column.adapterType(), column.adapterField())
            .addStatement("this.${column.adapterField()} = ${column.adapterField()}")
        copyConstructor.addParameter(column.adapterType(), column.adapterField())
            .addStatement("this.${column.adapterField()} = ${column.adapterField()}")
        marshal.addMethod(contentValuesMethod(column)
            .addModifiers(PUBLIC)
            .returns(TypeVariableName.get("T"))
            .addParameter(column.javaType, column.methodName)
            .addStatement("${column.adapterField()}.marshal($CONTENTVALUES_FIELD, " +
                "${column.constantName}, ${column.methodName})")
            .addStatement("return (T) this")
            .build())
      }
      copyConstructor.addStatement("this.${column.methodName}(copy.${column.methodName}())")
    }

    return marshal.addMethod(constructor.build()).addMethod(copyConstructor.build()).build()
  }

  private fun contentValuesMethod(column: SqliteParser.Column_defContext)
      = MethodSpec.methodBuilder(column.methodName)

  private fun marshalMethod(column: SqliteParser.Column_defContext) =
      if (column.isNullable && (column.type == ENUM || column.type == BOOLEAN)) {
        contentValuesMethod(column)
            .beginControlFlow("if (${column.methodName} == null)")
            .addStatement("$CONTENTVALUES_FIELD.putNull(${column.constantName})")
            .addStatement("return (T) this")
            .endControlFlow()
      } else {
        contentValuesMethod(column)
      }
          .addModifiers(PUBLIC)
          .addParameter(column.javaType, column.methodName)
          .returns(TypeVariableName.get("T"))
          .addStatement(
              "$CONTENTVALUES_FIELD.put(${column.constantName}, ${column.marshaledValue()})")
          .addStatement("return (T) this")
          .build()

  companion object {
    private val CONTENTVALUES_TYPE = ClassName.get("android.content", "ContentValues")
    private val CONTENTVALUES_FIELD = "contentValues"
    private val CONTENTVALUES_METHOD = "asContentValues"

    internal fun builder(
        table: SqliteParser.Create_table_stmtContext,
        interfaceClassName: ClassName,
        fileName: String
    ) = MarshalSpec(table, interfaceClassName, fileName)
  }
}
