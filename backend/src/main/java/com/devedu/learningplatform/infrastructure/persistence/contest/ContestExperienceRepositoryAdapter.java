package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.contest.rating.RatingCalculator.Change;
import com.devedu.learningplatform.domain.contest.VirtualContest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;

@Repository
public class ContestExperienceRepositoryAdapter implements ContestExperienceRepository {
    private final EntityManager em;
    private final ObjectMapper json;
    private final ContestRepository contests;
    public ContestExperienceRepositoryAdapter(EntityManager em, ObjectMapper json, ContestRepository contests) { this.em=em; this.json=json; this.contests=contests; }
    private Query query(String sql, Object... parameters) {
        var q=em.createNativeQuery(sql);
        for(int i=0;i<parameters.length;i+=2) q.setParameter((String)parameters[i],parameters[i+1]);
        return q;
    }
    @SuppressWarnings("unchecked") private List<Object[]> rows(String sql,Object... p) { return query(sql,p).getResultList(); }
    private Instant instant(Object value) {
        if(value instanceof Instant i) return i;
        if(value instanceof OffsetDateTime t) return t.toInstant();
        return ((java.sql.Timestamp)value).toInstant();
    }
    private String serialize(Object value) { try { return json.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException("Cannot store contest result",e); } }
    private ContestViews.Detail read(String value) { try { return json.readValue(value,ContestViews.Detail.class); } catch(Exception e) { throw new IllegalStateException("Cannot read contest result",e); } }
    @Override public boolean lockFinalization() { return (Boolean)query("select pg_try_advisory_xact_lock(684390123)").getSingleResult(); }
    @Override public boolean lockContestForFinalization(UUID id) { return (Boolean)query("select pg_try_advisory_xact_lock(hashtextextended(:key,0))","key","contest:"+id).getSingleResult(); }
    @Override @SuppressWarnings("unchecked") public List<UUID> awaitingFinalization(Instant now) {
        return query("select c.id from contests c left join contest_finalizations f on f.contest_id=c.id where c.ends_at <= :now and f.contest_id is null order by c.ends_at,c.id limit 20","now",now).getResultList();
    }
    @Override public Optional<ContestViews.Detail> finalSnapshot(UUID id) {
        var values=query("select snapshot from contest_finalizations where contest_id=:id","id",id).getResultList();
        return values.isEmpty()?Optional.empty():Optional.of(read((String)values.get(0)));
    }
    @Override public void saveSnapshot(UUID id,ContestViews.Detail snapshot,Instant now) {
        query("insert into contest_finalizations(contest_id,snapshot,finalized_at) values(:id,:snapshot,:now)","id",id,"snapshot",serialize(snapshot),"now",now).executeUpdate();
    }
    @Override public Map<Long,Integer> ratings(List<Long> ids) {
        var result=new HashMap<Long,Integer>(); if(ids.isEmpty()) return result;
        for(var row:rows("select u.public_id,r.rating from user_contest_ratings r join users u on u.id=r.user_id where u.public_id in (:ids)","ids",ids)) result.put(((Number)row[0]).longValue(),((Number)row[1]).intValue());
        return result;
    }
    @Override public void saveResult(UUID id,UUID user,ContestViews.Standing standing,Change change,Instant now) {
        query("insert into contest_results(contest_id,user_id,rank,score,solved,time_seconds,standing) values(:id,:user,:rank,:score,:solved,:time,:standing)",
                "id",id,"user",user,"rank",standing.rank(),"score",standing.score(),"solved",standing.solved(),"time",standing.timeSeconds(),"standing",serialize(standing)).executeUpdate();
        if(change==null) return;
        query("insert into rating_history(contest_id,user_id,previous_rating,new_rating,rating_change,calculated_at) values(:id,:user,:previous,:next,:change,:now)",
                "id",id,"user",user,"previous",change.previousRating(),"next",change.newRating(),"change",change.ratingChange(),"now",now).executeUpdate();
        query("insert into user_contest_ratings(user_id,rating,peak) values(:user,:rating,greatest(1200,:rating)) on conflict(user_id) do update set rating=excluded.rating,peak=greatest(user_contest_ratings.peak,excluded.peak)",
                "user",user,"rating",change.newRating()).executeUpdate();
    }
    @Override public Optional<Change> ratingChange(UUID id,UUID user) {
        return rows("select u.public_id,h.previous_rating,h.new_rating,h.rating_change from rating_history h join users u on u.id=h.user_id where h.contest_id=:id and h.user_id=:user","id",id,"user",user)
                .stream().map(this::change).findFirst();
    }
    private Change change(Object[] r) { return new Change(((Number)r[0]).longValue(),((Number)r[1]).intValue(),((Number)r[2]).intValue(),((Number)r[3]).intValue()); }
    @Override public List<ContestExperienceViews.History> history(UUID user) {
        return rows("select c.id,c.name,c.ends_at,r.rank,r.score,r.solved,r.time_seconds,u.public_id,h.previous_rating,h.new_rating,h.rating_change from contest_results r join contests c on c.id=r.contest_id join users u on u.id=r.user_id left join rating_history h on h.contest_id=r.contest_id and h.user_id=r.user_id where r.user_id=:user order by c.ends_at desc,c.id desc","user",user)
                .stream().map(r -> new ContestExperienceViews.History((UUID)r[0],(String)r[1],instant(r[2]),((Number)r[3]).intValue(),((Number)r[4]).intValue(),((Number)r[5]).intValue(),((Number)r[6]).longValue(),r[8]==null?null:change(Arrays.copyOfRange(r,7,11)))).toList();
    }
    @Override public List<ContestExperienceViews.VirtualHistory> virtualHistory(UUID user) {
        return rows("select v.id,v.contest_id,c.name,v.starts_at,v.ends_at from virtual_contests v join contests c on c.id=v.contest_id where v.user_id=:user order by v.starts_at desc","user",user)
                .stream().map(r -> new ContestExperienceViews.VirtualHistory((UUID)r[0],(UUID)r[1],(String)r[2],instant(r[3]),instant(r[4]))).toList();
    }
    @Override public void lockVirtualStart(UUID id,UUID user) { query("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))","key","virtual-start:"+id+":"+user).getSingleResult(); }
    private VirtualContest session(Object[] r) { return new VirtualContest((UUID)r[0],(UUID)r[1],(UUID)r[2],instant(r[3]),instant(r[4])); }
    @Override public Optional<VirtualContest> latestVirtual(UUID id,UUID user) {
        return rows("select id,contest_id,user_id,starts_at,ends_at from virtual_contests where contest_id=:id and user_id=:user order by starts_at desc limit 1","id",id,"user",user).stream().map(this::session).findFirst();
    }
    @Override public Optional<VirtualContest> virtual(UUID id,UUID user) {
        return rows("select id,contest_id,user_id,starts_at,ends_at from virtual_contests where id=:id and user_id=:user","id",id,"user",user).stream().map(this::session).findFirst();
    }
    @Override public VirtualContest saveVirtual(VirtualContest v) {
        query("insert into virtual_contests(id,contest_id,user_id,starts_at,ends_at) values(:id,:contest,:user,:start,:end)","id",v.id(),"contest",v.contestId(),"user",v.userId(),"start",v.startsAt(),"end",v.endsAt()).executeUpdate(); return v;
    }
    @Override public List<ContestRepository.Entry> virtualEntries(UUID id) { return contests.virtualEntries(id); }
}
