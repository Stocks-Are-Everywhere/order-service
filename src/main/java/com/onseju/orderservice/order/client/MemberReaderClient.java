package com.onseju.orderservice.order.client;

import com.onseju.orderservice.grpc.MemberProto;
import com.onseju.orderservice.grpc.MemberReaderServiceGrpc;
import com.onseju.orderservice.tradehistory.dto.ReadMemberDto;
import com.onseju.orderservice.tradehistory.dto.ReadMemberResponse;
import lombok.AllArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class MemberReaderClient {

    @GrpcClient("order-service")
    private final MemberReaderServiceGrpc.MemberReaderServiceBlockingStub memberReaderServiceBlockingStub;

    public ReadMemberResponse readMember(ReadMemberDto dto) {
        try {

            MemberProto.GrpcReadMemberRequest request = MemberProto.GrpcReadMemberRequest.newBuilder()
                    .setAccountId(dto.accountId())
                    .build();


            // gRPC 응답을 ValidateResponse 객체로 변환
            MemberProto.GrpcReadMemberResponse response = memberReaderServiceBlockingStub.readMember(request);

            return new ReadMemberResponse(response.getMemberId());
        } catch (Exception e) {
            throw new RuntimeException("gRPC 서비스 통신 오류", e);
        }
    }
}
