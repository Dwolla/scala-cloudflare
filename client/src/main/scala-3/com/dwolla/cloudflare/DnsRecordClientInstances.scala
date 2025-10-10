package com.dwolla.cloudflare

import cats.*
import cats.tagless.aop.*
import com.dwolla.cloudflare.domain.model.*
import com.dwolla.tracing.LowPriorityTraceableValueInstances.*
import natchez.TraceableValue

trait DnsRecordClientInstances:
  given Aspect[DnsRecordClient, TraceableValue, TraceableValue] =
    new Aspect[DnsRecordClient, TraceableValue, TraceableValue]:
      override def weave[F[_]](af: DnsRecordClient[F]): DnsRecordClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]] =
        new DnsRecordClient[[a] =>> Aspect.Weave[F, TraceableValue, TraceableValue, a]]:
          override def getById(zoneId: ZoneId, resourceId: ResourceId): Aspect.Weave[F, TraceableValue, TraceableValue, IdentifiedDnsRecord] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("zoneId", zoneId),
                Aspect.Advice.byValue("resourceId", resourceId),
              )),
              Aspect.Advice("getById", af.getById(zoneId, resourceId))
            )

          override def createDnsRecord(record: UnidentifiedDnsRecord): Aspect.Weave[F, TraceableValue, TraceableValue, IdentifiedDnsRecord] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("record", record),
              )),
              Aspect.Advice("createDnsRecord", af.createDnsRecord(record))
            )

          override def updateDnsRecord(record: IdentifiedDnsRecord): Aspect.Weave[F, TraceableValue, TraceableValue, IdentifiedDnsRecord] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("record", record),
              )),
              Aspect.Advice("updateDnsRecord", af.updateDnsRecord(record))
            )

          override def getExistingDnsRecords(name: String, content: Option[String], recordType: Option[String]): Aspect.Weave[F, TraceableValue, TraceableValue, IdentifiedDnsRecord] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("name", name),
                Aspect.Advice.byValue("content", content),
                Aspect.Advice.byValue("recordType", recordType),
              )),
              Aspect.Advice("getExistingDnsRecords", af.getExistingDnsRecords(name, content, recordType))
            )

          override def deleteDnsRecord(physicalResourceId: String): Aspect.Weave[F, TraceableValue, TraceableValue, PhysicalResourceId] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("physicalResourceId", physicalResourceId),
              )),
              Aspect.Advice("deleteDnsRecord", af.deleteDnsRecord(physicalResourceId))
            )

          @annotation.targetName("deleteDnsRecordNewtype")
          override def deleteDnsRecord(physicalResourceId: PhysicalResourceId): Aspect.Weave[F, TraceableValue, TraceableValue, PhysicalResourceId] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("physicalResourceId", physicalResourceId),
              )),
              Aspect.Advice("deleteDnsRecord", af.deleteDnsRecord(physicalResourceId))
            )

          override def getByUri(uri: String): Aspect.Weave[F, TraceableValue, TraceableValue, IdentifiedDnsRecord] =
            Aspect.Weave(
              "DnsRecordClient",
              List(List(
                Aspect.Advice.byValue("uri", uri),
              )),
              Aspect.Advice("getByUri", af.getByUri(uri))
            )

      override def mapK[F[_], G[_]](af: DnsRecordClient[F])(fk: F ~> G): DnsRecordClient[G] =
        new DnsRecordClient[G]:
          override def getById(zoneId: ZoneId, resourceId: ResourceId): G[IdentifiedDnsRecord] =
            fk(af.getById(zoneId, resourceId))

          override def createDnsRecord(record: UnidentifiedDnsRecord): G[IdentifiedDnsRecord] =
            fk(af.createDnsRecord(record))

          override def updateDnsRecord(record: IdentifiedDnsRecord): G[IdentifiedDnsRecord] =
            fk(af.updateDnsRecord(record))

          override def getExistingDnsRecords(name: String, content: Option[String], recordType: Option[String]): G[IdentifiedDnsRecord] =
            fk(af.getExistingDnsRecords(name, content, recordType))

          override def deleteDnsRecord(physicalResourceId: String): G[PhysicalResourceId] =
            fk(af.deleteDnsRecord(physicalResourceId))

          @annotation.targetName("deleteDnsRecordNewtype")
          override def deleteDnsRecord(physicalResourceId: PhysicalResourceId): G[PhysicalResourceId] =
            fk(af.deleteDnsRecord(physicalResourceId))

          override def getByUri(uri: String): G[IdentifiedDnsRecord] =
            fk(af.getByUri(uri))
