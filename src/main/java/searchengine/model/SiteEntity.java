package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Data
@Entity
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;


//    @OneToMany(mappedBy = "siteEntity",  cascade = CascadeType.ALL)
//    Set<PageEntity> pageEntity = new HashSet<>();

    //@Column(columnDefinition = "ENUM")
    //@Column(columnDefinition = "ENUM('INDEXING', 'INDEXED')")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusType status;

    @Column(nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    public SiteEntity() {}
}
