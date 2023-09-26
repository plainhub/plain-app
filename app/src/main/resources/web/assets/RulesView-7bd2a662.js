import{d as le,B as ce,r as A,u as se,a1 as J,am as R,bW as T,bX as re,ai as ue,G as pe,aj as F,o as r,e as u,f as e,t as o,h as c,L as _,bY as V,bZ as L,F as w,A as C,R as H,ak as P,x as N,y as x,j as O,ap as me,i as _e,k as ve,a7 as j,b_ as fe,b$ as he,K as ge,X as B,T as ee,g as te,U as oe,w as ne,a8 as $e,$ as be}from"./index-391e08bf.js";import{_ as ke}from"./Breadcrumb-db40a1f3.js";import{T as $,a as b,_ as ye,A as we}from"./question-mark-rounded-3c39ae47.js";import{u as Ce,a as Te}from"./vee-validate.esm-910387f0.js";const Fe={slot:"headline"},Ve={slot:"content"},Ae={class:"row"},Ne={class:"col-md-3 col-form-label"},De={class:"col-md-9 form-checks"},Ie={class:"form-check form-check-inline"},Me={class:"form-check-label",for:"action-allow"},Ue={class:"form-check form-check-inline"},Ee={class:"form-check-label",for:"action-block"},Se={class:"row mb-2"},qe={for:"action",class:"col-md-3 col-form-label"},Re={class:"col-md-9 form-checks"},Le={class:"form-check form-check-inline"},Oe={class:"form-check-label",for:"direction-inbound"},je={class:"form-check form-check-inline"},Be={class:"form-check-label",for:"direction-outbound"},Je={class:"row mb-3"},Ge={class:"col-md-3 col-form-label"},Qe={class:"col-md-9"},Xe=["value"],Ke={key:0,class:"input-group mt-2"},We=["placeholder"],Ye={class:"inner"},Ze={class:"help-block"},ze={value:""},He=["value"],Pe={key:2,class:"invalid-feedback"},xe={class:"row mb-3"},et={class:"col-md-3 col-form-label"},tt={class:"col-md-9"},ot={value:"all"},nt=["value"],at=["value"],lt={class:"row mb-3"},st={class:"col-md-3 col-form-label"},it={class:"col-md-9"},dt={slot:"actions"},ct=["disabled"],ae=le({__name:"EditRuleModal",props:{data:{type:Object},devices:{type:Array},networks:{type:Array}},setup(k){var y,G,Q,X,K,W,Y;const f=k,{handleSubmit:g}=Ce(),l=ce({action:"block",direction:"inbound",protocol:"all",apply_to:"all",notes:"",target:"",is_enabled:!0}),p=A($.DNS),D=Object.values($),{t:I}=se(),{mutate:M,loading:U,onDone:E}=J({document:R`
    mutation createConfig($input: ConfigInput!) {
      createConfig(input: $input) {
        ...ConfigFragment
      }
    }
    ${T}
  `,options:{update:(n,s)=>{re(n,s.data.createConfig,R`
          query {
            configs {
              ...ConfigFragment
            }
          }
          ${T}
        `)}}}),{mutate:S,loading:q,onDone:a}=J({document:R`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${T}
  `}),{value:m,resetField:v,errorMessage:h}=Te("inputValue",ue().test("required",n=>"valid.required",n=>!b.hasInput(p.value)||!!n).test("target-value",n=>"invalid_value",n=>b.isValid(p.value,n??""))),i=(y=f.data)==null?void 0:y.data;l.action=(i==null?void 0:i.action)??"block",l.direction=(i==null?void 0:i.direction)??"inbound",l.protocol=(i==null?void 0:i.protocol)??"all",p.value=((Q=(G=f.data)==null?void 0:G.target)==null?void 0:Q.type)??$.DNS,m.value=((K=(X=f.data)==null?void 0:X.target)==null?void 0:K.value)??"",l.apply_to=((Y=(W=f.data)==null?void 0:W.applyTo)==null?void 0:Y.toValue())??"all",l.notes=(i==null?void 0:i.notes)??"",l.is_enabled=(i==null?void 0:i.is_enabled)??!0,i||v(),pe(p,(n,s)=>{(n===$.INTERFACE||s===$.INTERFACE)&&(m.value="")});const d=g(()=>{const n=new b;n.type=p.value,n.value=m.value??"",l.target=n.toValue(),f.data?S({id:f.data.id,input:{group:"rule",value:JSON.stringify(l)}}):M({input:{group:"rule",value:JSON.stringify(l)}})});return E(()=>{F()}),a(()=>{F()}),(n,s)=>{var Z,z;const ie=ye,de=me;return r(),u("md-dialog",null,[e("div",Fe,o(c(i)?n.$t("edit"):n.$t("create")),1),e("div",Ve,[e("div",Ae,[e("label",Ne,o(n.$t("actions")),1),e("div",De,[e("div",Ie,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-allow",value:"allow","onUpdate:modelValue":s[0]||(s[0]=t=>l.action=t)},null,512),[[V,l.action]]),e("label",Me,o(n.$t("allow")),1)]),e("div",Ue,[_(e("input",{class:"form-check-input",type:"radio",name:"action",id:"action-block",value:"block","onUpdate:modelValue":s[1]||(s[1]=t=>l.action=t)},null,512),[[V,l.action]]),e("label",Ee,o(n.$t("block")),1)])])]),e("div",Se,[e("label",qe,o(n.$t("direction")),1),e("div",Re,[e("div",Le,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-inbound",value:"inbound","onUpdate:modelValue":s[2]||(s[2]=t=>l.direction=t)},null,512),[[V,l.direction]]),e("label",Oe,o(n.$t("inbound")),1)]),e("div",je,[_(e("input",{class:"form-check-input",type:"radio",name:"direction",id:"direction-outbound",value:"outbound","onUpdate:modelValue":s[3]||(s[3]=t=>l.direction=t)},null,512),[[V,l.direction]]),e("label",Be,o(n.$t("outbound")),1)])])]),e("div",Je,[e("label",Ge,o(n.$t("match")),1),e("div",Qe,[_(e("select",{class:"form-select","onUpdate:modelValue":s[4]||(s[4]=t=>p.value=t)},[(r(!0),u(w,null,C(c(D),t=>(r(),u("option",{value:t},o(n.$t(`target_type.${t}`)),9,Xe))),256))],512),[[L,p.value]]),c(b).hasInput(p.value)?(r(),u("div",Ke,[_(e("input",{type:"text",class:"form-control","onUpdate:modelValue":s[5]||(s[5]=t=>P(m)?m.value=t:null),placeholder:n.$t("for_example")+" "+c(b).hint(p.value)},null,8,We),[[H,c(m)]]),N(de,{class:"input-group-text"},{content:x(()=>[e("pre",Ze,o(n.$t(`examples_${p.value}`)),1)]),default:x(()=>[e("span",Ye,[N(ie)])]),_:1})])):O("",!0),p.value===c($).INTERFACE?_((r(),u("select",{key:1,class:"form-select mt-2","onUpdate:modelValue":s[6]||(s[6]=t=>P(m)?m.value=t:null)},[e("option",ze,o(n.$t("all_local_networks")),1),(r(!0),u(w,null,C((Z=k.networks)==null?void 0:Z.filter(t=>t.type!=="wan"),t=>(r(),u("option",{value:t.ifName},o(t.name),9,He))),256))],512)),[[L,c(m)]]):O("",!0),c(h)?(r(),u("div",Pe,o(c(h)?n.$t(c(h)):""),1)):O("",!0)])]),e("div",xe,[e("label",et,o(c(I)("apply_to")),1),e("div",tt,[_(e("select",{class:"form-select","onUpdate:modelValue":s[7]||(s[7]=t=>l.apply_to=t)},[e("option",ot,o(n.$t("all_devices")),1),(r(!0),u(w,null,C((z=k.networks)==null?void 0:z.filter(t=>t.type!=="wan"),t=>(r(),u("option",{key:t.ifName,value:"iface:"+t.ifName},o(t.name),9,nt))),128)),(r(!0),u(w,null,C(k.devices,t=>(r(),u("option",{value:"mac:"+t.mac},o(t.name),9,at))),256))],512),[[L,l.apply_to]])])]),e("div",lt,[e("label",st,o(n.$t("notes")),1),e("div",it,[_(e("textarea",{class:"form-control","onUpdate:modelValue":s[8]||(s[8]=t=>l.notes=t),rows:"3"},null,512),[[H,l.notes]])])])]),e("div",dt,[e("md-outlined-button",{value:"cancel",onClick:s[9]||(s[9]=(...t)=>c(F)&&c(F)(...t))},o(n.$t("cancel")),1),e("md-filled-button",{value:"save",disabled:c(U)||c(q),onClick:s[10]||(s[10]=(...t)=>c(d)&&c(d)(...t)),autofocus:""},o(n.$t("save")),9,ct)])])}}}),rt={class:"page-container"},ut={class:"main"},pt={class:"v-toolbar"},mt={class:"table-responsive"},_t={class:"table"},vt=e("th",null,"ID",-1),ft={class:"actions two"},ht={class:"form-check"},gt=["disabled","onChange","checked"],$t={class:"nowrap"},bt={class:"nowrap"},kt={class:"actions two"},yt=["onClick"],wt=["onClick"],At=le({__name:"RulesView",setup(k){const f=A([]),g=A([]),l=A([]),{t:p}=se();_e({handle:(a,m)=>{m?ve(p(m),"error"):(f.value=a.configs.filter(v=>v.group==="rule").map(v=>{const h=JSON.parse(v.value),i=new we;i.parse(h.apply_to);const d=new b;return d.parse(h.target),{id:v.id,createdAt:v.createdAt,updatedAt:v.updatedAt,data:h,applyTo:i,target:d}}),g.value=[...a.devices],l.value=[...a.networks])},document:j`
    query {
      configs {
        ...ConfigFragment
      }
      devices {
        ...DeviceFragment
      }
      networks {
        ...NetworkFragment
      }
    }
    ${fe}
    ${T}
    ${he}
  `});const D=j`
  mutation DeleteConfig($id: ID!) {
    deleteConfig(id: $id)
  }
`;function I(a){B($e,{id:a.id,name:a.id,gql:D,appApi:!1,typeName:"Config"})}function M(a){B(ae,{data:a,devices:g,networks:l})}function U(){B(ae,{data:null,devices:g,networks:l})}const{mutate:E,loading:S}=J({document:j`
    mutation updateConfig($id: ID!, $input: ConfigInput!) {
      updateConfig(id: $id, input: $input) {
        ...ConfigFragment
      }
    }
    ${T}
  `});function q(a){E({id:a.id,input:{group:"rule",value:JSON.stringify(a.data)}})}return(a,m)=>{const v=ke,h=be,i=ge("tooltip");return r(),u("div",rt,[e("div",ut,[e("div",pt,[N(v,{current:()=>a.$t("page_title.rules")},null,8,["current"]),e("button",{type:"button",class:"btn right-actions",onClick:U},o(a.$t("create")),1)]),e("div",mt,[e("table",_t,[e("thead",null,[e("tr",null,[vt,e("th",null,o(a.$t("apply_to")),1),e("th",null,o(a.$t("description")),1),e("th",null,o(a.$t("notes")),1),e("th",null,o(a.$t("enabled")),1),e("th",null,o(a.$t("created_at")),1),e("th",null,o(a.$t("updated_at")),1),e("th",ft,o(a.$t("actions")),1)])]),e("tbody",null,[(r(!0),u(w,null,C(f.value,d=>(r(),u("tr",{key:d.id},[e("td",null,[N(h,{id:d.id,raw:d.data},null,8,["id","raw"])]),e("td",null,o(d.applyTo.getText(a.$t,g.value,l.value)),1),e("td",null,o(a.$t(`rule_${d.data.direction}`,{action:a.$t(d.data.action),target:d.target.getText(a.$t,l.value)})),1),e("td",null,o(d.data.notes),1),e("td",null,[e("div",ht,[e("md-checkbox",{"touch-target":"wrapper",disabled:c(S),onChange:y=>q(d),checked:d.data.is_enabled},null,40,gt)])]),e("td",$t,[_((r(),u("span",null,[te(o(c(oe)(d.createdAt)),1)])),[[i,c(ee)(d.createdAt)]])]),e("td",bt,[_((r(),u("span",null,[te(o(c(oe)(d.updatedAt)),1)])),[[i,c(ee)(d.updatedAt)]])]),e("td",kt,[e("a",{href:"#",class:"v-link",onClick:ne(y=>M(d),["prevent"])},o(a.$t("edit")),9,yt),e("a",{href:"#",class:"v-link",onClick:ne(y=>I(d),["prevent"])},o(a.$t("delete")),9,wt)])]))),128))])])])])])}}});export{At as default};
