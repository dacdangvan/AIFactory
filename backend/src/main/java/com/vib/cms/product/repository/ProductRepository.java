package com.vib.cms.product.repository;
import com.vib.cms.product.api.*;import com.vib.cms.common.exception.ResourceNotFoundException;import org.springframework.jdbc.core.*;import org.springframework.stereotype.Repository;import java.util.*;
@Repository public class ProductRepository {
 private final JdbcTemplate jdbc; private final RowMapper<ProductResponse> mapper=(r,n)->new ProductResponse(r.getObject("id",UUID.class),r.getString("code"),r.getString("name"),r.getString("description"),r.getString("status"),r.getObject("created_at",java.time.OffsetDateTime.class),r.getObject("updated_at",java.time.OffsetDateTime.class));
 public ProductRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public List<ProductResponse> findAll(){return jdbc.query("select id,code,name,description,status,created_at,updated_at from products order by name",mapper);}
 public ProductResponse find(UUID id){return jdbc.query("select id,code,name,description,status,created_at,updated_at from products where id=?",mapper,id).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Product not found: "+id));}
 public ProductResponse insert(ProductRequest r,String actor){return jdbc.queryForObject("insert into products(code,name,description,status,created_by,updated_by) values (?,?,?,coalesce(?,'ACTIVE'),?,?) returning id,code,name,description,status,created_at,updated_at",mapper,r.code(),r.name(),r.description(),r.status(),actor,actor);}
 public ProductResponse update(UUID id,ProductRequest r,String actor){return jdbc.query("update products set code=?,name=?,description=?,status=coalesce(?,status),updated_at=now(),updated_by=? where id=? returning id,code,name,description,status,created_at,updated_at",mapper,r.code(),r.name(),r.description(),r.status(),actor,id).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Product not found: "+id));}
 public void archive(UUID id){if(jdbc.update("update products set status='ARCHIVED',updated_at=now() where id=?",id)==0)throw new ResourceNotFoundException("Product not found: "+id);}
}
