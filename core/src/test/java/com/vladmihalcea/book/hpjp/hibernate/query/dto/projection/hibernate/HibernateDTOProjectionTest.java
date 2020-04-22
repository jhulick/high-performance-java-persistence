package com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.hibernate;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.Post;
import com.vladmihalcea.book.hpjp.hibernate.query.dto.projection.jpa.JPADTOProjectionTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.hibernate.type.util.ListResultTransformer;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("unchecked")
public class HibernateDTOProjectionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setCreatedBy("Vlad Mihalcea")
                    .setCreatedOn(
                        LocalDateTime.of(2016, 11, 2, 12, 0, 0)
                    )
                    .setUpdatedBy("Vlad Mihalcea")
                    .setUpdatedOn(
                        LocalDateTime.now()
                    )
            );
        });
    }

    @Test
    public void testJPQLResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createQuery("""
                select
                   p.id as id,
                   p.title as title
                from Post p
                """)
            .unwrap(org.hibernate.query.Query.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testNativeQueryResultTransformer() {
        doInJPA( entityManager -> {
            List<PostDTO> postDTOs = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "id",
                   p.title AS "title"
                FROM post p
                """)
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setResultTransformer(Transformers.aliasToBean(PostDTO.class))
            .getResultList();

            assertEquals(1, postDTOs.size());

            PostDTO postDTO = postDTOs.get(0);
            assertEquals(1L, postDTO.getId().longValue());
            assertEquals("High-Performance Java Persistence", postDTO.getTitle());
        } );
    }

    @Test
    public void testRecord() {
        doInJPA(entityManager -> {
            List<PostRecord> postRecords = entityManager.createQuery("""
                select 
                    p.id,
                    p.title,
                    p.createdOn,
                    p.createdBy,
                    p.updatedOn,
                    p.updatedBy
                from Post p
                """)
            .unwrap(Query.class)
            .setResultTransformer(
                (ListResultTransformer) (tuple, aliases) -> {
                    int i =0;
                    return new PostRecord(
                        ((Number) tuple[i++]).longValue(),
                        (String) tuple[i++],
                        new AuditRecord(
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++],
                            (LocalDateTime) tuple[i++],
                            (String) tuple[i++]
                        )
                    );
                }
            )
            .getResultList();

            assertEquals(1, postRecords.size());

            PostRecord postRecord = postRecords.get(0);

            assertEquals(
                1L, postRecord.id().longValue()
            );

            assertEquals(
                "High-Performance Java Persistence", postRecord.title()
            );

            assertEquals(
                LocalDateTime.of(2016, 11, 2, 12, 0, 0), postRecord.audit().createdOn()
            );

            assertEquals(
                "Vlad Mihalcea", postRecord.audit().createdBy()
            );
        });
    }

    public static class PostDTO {

        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Number id) {
            this.id = id.longValue();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static record PostRecord(
        Long id,
        String title,
        AuditRecord audit
    ) {
    }

    public static record AuditRecord(
        LocalDateTime createdOn,
        String createdBy,
        LocalDateTime updatedOn,
        String updatedBy
    ) {
    }
}
